package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.ChatConstant;
import com.xueyifang.domain.dto.chat.ChatSendRequest;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.UserChat;
import com.xueyifang.domain.vo.ChatConversationVO;
import com.xueyifang.domain.vo.ChatMessageVO;
import com.xueyifang.domain.vo.UserSimpleVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.UserChatMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.IChatService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.utils.UserContext;
import com.xueyifang.websocket.WebSocketMessage;
import com.xueyifang.websocket.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户聊天记录表 服务实现类
 * </p>
 *
 * @author xueyifang
 * @since 2025-11-24
 */
@Slf4j
@Service
public class ChatServiceImpl extends ServiceImpl<UserChatMapper, UserChat> implements IChatService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private WebSocketSessionManager webSocketSessionManager;

    @Resource
    private com.xueyifang.utils.FileUrlResolver fileUrlResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(ChatSendRequest request) {
        Long senderId = UserContext.getCurrentUserId();
        if (senderId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        Long receiverId = request.getReceiverId();

        // 校验：不能给自己发消息
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.CHAT_CANNOT_SEND_TO_SELF);
        }

        // 校验：接收者必须存在且账户正常
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) {
            throw new BusinessException(ErrorCode.CHAT_RECEIVER_NOT_EXIST);
        }
        if (receiver.getAccountStatus() != null && receiver.getAccountStatus() == 2) {
            throw new BusinessException(ErrorCode.CHAT_RECEIVER_ACCOUNT_DISABLED);
        }

        // 创建聊天记录
        int msgType = request.getMessageType() != null ? request.getMessageType() : ChatConstant.MESSAGE_TYPE_TEXT;
        String storedContent = request.getContent();
        if (msgType == ChatConstant.MESSAGE_TYPE_IMAGE && fileUrlResolver != null) {
            storedContent = fileUrlResolver.toRelativeKey(storedContent);
        }

        UserChat userChat = new UserChat();
        userChat.setSenderId(senderId);
        userChat.setReceiverId(receiverId);
        userChat.setContent(storedContent);
        userChat.setMessageType(msgType);
        userChat.setIsRead(ChatConstant.READ_STATUS_UNREAD);
        userChat.setRelatedServiceId(request.getRelatedServiceId());
        userChat.setRelatedOrderId(request.getRelatedOrderId());
        userChat.setCreateTime(LocalDateTime.now());
        userChat.setUpdateTime(LocalDateTime.now());

        this.save(userChat);

        // 事务提交后再通过 WebSocket 推送（失败不影响主流程）
        com.xueyifang.utils.TransactionUtils.afterCommit(() -> {
            try {
                pushMessageToReceiver(userChat, senderId, receiver);
            } catch (Exception e) {
                log.warn("WebSocket推送消息失败: receiverId={}", receiver.getId(), e);
            }
        });
    }

    /**
     * 通过 WebSocket 推送消息给接收者
     */
    private void pushMessageToReceiver(UserChat userChat, Long senderId, User receiver) {
        // 构建消息VO
        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setMessageId(userChat.getId());
        if (Integer.valueOf(ChatConstant.MESSAGE_TYPE_IMAGE).equals(userChat.getMessageType()) && fileUrlResolver != null) {
            messageVO.setContent(fileUrlResolver.toPublicUrl(userChat.getContent()));
        } else {
            messageVO.setContent(userChat.getContent());
        }
        messageVO.setMessageType(userChat.getMessageType());
        messageVO.setIsRead(userChat.getIsRead());
        messageVO.setRelatedServiceId(userChat.getRelatedServiceId());
        messageVO.setRelatedOrderId(userChat.getRelatedOrderId());
        messageVO.setCreateTime(userChat.getCreateTime());

        // 设置发送者信息
        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            UserSimpleVO senderVO = new UserSimpleVO();
            senderVO.setUserId(sender.getId());
            senderVO.setRealName(sender.getRealName());
            senderVO.setAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(sender.getAvatar()) : sender.getAvatar());
            messageVO.setSender(senderVO);
        }

        // 设置接收者信息
        UserSimpleVO receiverVO = new UserSimpleVO();
        receiverVO.setUserId(receiver.getId());
        receiverVO.setRealName(receiver.getRealName());
        receiverVO.setAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(receiver.getAvatar()) : receiver.getAvatar());
        messageVO.setReceiver(receiverVO);

        // 推送给接收者
        WebSocketMessage wsMessage = WebSocketMessage.newChat(messageVO);
        webSocketSessionManager.sendToUser(receiver.getId(), wsMessage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Page<ChatMessageVO> getChatMessagesWithUser(Long targetUserId, Integer pageNum, Integer pageSize) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        // 校验目标用户是否存在
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        // 查询双方的聊天记录
        Page<UserChat> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserChat> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .and(w1 -> w1.eq(UserChat::getSenderId, currentUserId).eq(UserChat::getReceiverId, targetUserId))
                .or(w2 -> w2.eq(UserChat::getSenderId, targetUserId).eq(UserChat::getReceiverId, currentUserId))
        );
        queryWrapper.orderByAsc(UserChat::getCreateTime);  // 按时间升序排序

        Page<UserChat> chatPage = this.page(page, queryWrapper);

        // 收集所有涉及的用户ID
        Set<Long> userIds = new HashSet<>();
        userIds.add(currentUserId);
        userIds.add(targetUserId);

        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));
        }

        // 获取当前用户和目标用户
        User currentUser = userMap.get(currentUserId);
        Map<Long, User> finalUserMap = userMap;

        // 转换为VO
        List<ChatMessageVO> voList = chatPage.getRecords().stream().map(chat -> {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setMessageId(chat.getId());
            if (Integer.valueOf(ChatConstant.MESSAGE_TYPE_IMAGE).equals(chat.getMessageType()) && fileUrlResolver != null) {
                vo.setContent(fileUrlResolver.toPublicUrl(chat.getContent()));
            } else {
                vo.setContent(chat.getContent());
            }
            vo.setMessageType(chat.getMessageType());
            vo.setIsRead(chat.getIsRead());
            vo.setRelatedServiceId(chat.getRelatedServiceId());
            vo.setRelatedOrderId(chat.getRelatedOrderId());
            vo.setCreateTime(chat.getCreateTime());

            // 设置发送者信息
            User sender = finalUserMap.get(chat.getSenderId());
            if (sender != null) {
                UserSimpleVO senderVO = new UserSimpleVO();
                senderVO.setUserId(sender.getId());
                senderVO.setRealName(sender.getRealName());
                senderVO.setAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(sender.getAvatar()) : sender.getAvatar());
                vo.setSender(senderVO);
            }

            // 设置接收者信息
            User receiver = finalUserMap.get(chat.getReceiverId());
            if (receiver != null) {
                UserSimpleVO receiverVO = new UserSimpleVO();
                receiverVO.setUserId(receiver.getId());
                receiverVO.setRealName(receiver.getRealName());
                receiverVO.setAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(receiver.getAvatar()) : receiver.getAvatar());
                vo.setReceiver(receiverVO);
            }

            return vo;
        }).collect(Collectors.toList());

        // 标记对方发来的未读消息为已读
        LambdaUpdateWrapper<UserChat> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserChat::getReceiverId, currentUserId)
                .eq(UserChat::getSenderId, targetUserId)
                .eq(UserChat::getIsRead, ChatConstant.READ_STATUS_UNREAD)
                .set(UserChat::getIsRead, ChatConstant.READ_STATUS_READ)
                .set(UserChat::getUpdateTime, LocalDateTime.now());
        this.update(updateWrapper);

        // 构造返回结果
        Page<ChatMessageVO> resultPage = new Page<>(pageNum, pageSize, chatPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public List<ChatConversationVO> getMyConversations() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        // 使用 SQL 分组聚合查询，替代内存分组
        List<ChatConversationVO> conversations = baseMapper.selectConversations(currentUserId);
        if (conversations != null && fileUrlResolver != null) {
            for (ChatConversationVO conv : conversations) {
                if (conv.getAvatar() != null && !conv.getAvatar().isEmpty()) {
                    conv.setAvatar(fileUrlResolver.toPublicUrl(conv.getAvatar()));
                }
            }
        }
        return conversations;
    }
}
