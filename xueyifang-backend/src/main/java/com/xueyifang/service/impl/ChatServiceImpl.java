package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.ChatConstant;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.dto.chat.ChatSendRequest;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.po.UserChat;
import com.xueyifang.domain.vo.ChatConversationVO;
import com.xueyifang.domain.vo.ChatMessageVO;
import com.xueyifang.domain.vo.UserSimpleVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.UserChatMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.IChatService;
import com.xueyifang.utils.ChatAntiBypassUtils;
import com.xueyifang.utils.FileUrlResolver;
import com.xueyifang.utils.TransactionUtils;
import com.xueyifang.utils.UserContext;
import com.xueyifang.websocket.WebSocketMessage;
import com.xueyifang.websocket.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl extends ServiceImpl<UserChatMapper, UserChat> implements IChatService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private WebSocketSessionManager webSocketSessionManager;

    @Resource
    private FileUrlResolver fileUrlResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(ChatSendRequest request) {
        Long senderId = UserContext.getCurrentUserId();
        if (senderId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        Long receiverId = request.getReceiverId();
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.CHAT_CANNOT_SEND_TO_SELF);
        }

        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) {
            throw new BusinessException(ErrorCode.CHAT_RECEIVER_NOT_EXIST);
        }
        if (receiver.getAccountStatus() != null && receiver.getAccountStatus() == 2) {
            throw new BusinessException(ErrorCode.CHAT_RECEIVER_ACCOUNT_DISABLED);
        }

        int msgType = request.getMessageType() != null ? request.getMessageType() : ChatConstant.MESSAGE_TYPE_TEXT;
        String storedContent = request.getContent();

        if (msgType == ChatConstant.MESSAGE_TYPE_IMAGE && fileUrlResolver != null) {
            storedContent = fileUrlResolver.toRelativeKey(storedContent);
        } else if (msgType == ChatConstant.MESSAGE_TYPE_TEXT) {
            boolean hasPaidEscrow = hasActivePaidEscrowBetween(senderId, receiverId);
            ChatAntiBypassUtils.SanitizeResult inspected = ChatAntiBypassUtils.inspectAndSanitize(storedContent, hasPaidEscrow);
            if (inspected.isBlocked()) {
                throw new BusinessException(ErrorCode.CHAT_BYPASS_BLOCKED, inspected.getRiskNotice());
            }
            storedContent = inspected.getSanitizedContent();
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

        TransactionUtils.afterCommit(() -> {
            try {
                pushMessageToReceiver(userChat, senderId, receiver);
            } catch (Exception e) {
                log.warn("WebSocket推送消息失败: receiverId={}", receiver.getId(), e);
            }
        });
    }

    private boolean hasActivePaidEscrowBetween(Long userA, Long userB) {
        if (serviceOrderMapper == null) {
            return false;
        }
        Long count = serviceOrderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getPaymentStatus, OrderConstant.PAYMENT_PAID)
                .in(ServiceOrder::getOrderStatus, OrderConstant.ORDER_PENDING_SHIP, OrderConstant.ORDER_PENDING_RECEIPT)
                .and(w -> w.and(p -> p.eq(ServiceOrder::getBuyerId, userA).eq(ServiceOrder::getSellerId, userB))
                        .or(p -> p.eq(ServiceOrder::getBuyerId, userB).eq(ServiceOrder::getSellerId, userA))));
        return count != null && count > 0;
    }

    private void pushMessageToReceiver(UserChat userChat, Long senderId, User receiver) {
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

        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            UserSimpleVO senderVO = new UserSimpleVO();
            senderVO.setUserId(sender.getId());
            senderVO.setRealName(sender.getRealName());
            senderVO.setAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(sender.getAvatar()) : sender.getAvatar());
            messageVO.setSender(senderVO);
        }

        UserSimpleVO receiverVO = new UserSimpleVO();
        receiverVO.setUserId(receiver.getId());
        receiverVO.setRealName(receiver.getRealName());
        receiverVO.setAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(receiver.getAvatar()) : receiver.getAvatar());
        messageVO.setReceiver(receiverVO);

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
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        Page<UserChat> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserChat> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .and(w1 -> w1.eq(UserChat::getSenderId, currentUserId).eq(UserChat::getReceiverId, targetUserId))
                .or(w2 -> w2.eq(UserChat::getSenderId, targetUserId).eq(UserChat::getReceiverId, currentUserId))
        );
        queryWrapper.orderByAsc(UserChat::getCreateTime);
        Page<UserChat> chatPage = this.page(page, queryWrapper);

        Set<Long> userIds = new HashSet<>();
        userIds.add(currentUserId);
        userIds.add(targetUserId);
        Map<Long, User> finalUserMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

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

            User sender = finalUserMap.get(chat.getSenderId());
            if (sender != null) {
                UserSimpleVO senderVO = new UserSimpleVO();
                senderVO.setUserId(sender.getId());
                senderVO.setRealName(sender.getRealName());
                senderVO.setAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(sender.getAvatar()) : sender.getAvatar());
                vo.setSender(senderVO);
            }
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

        this.update(new LambdaUpdateWrapper<UserChat>()
                .eq(UserChat::getReceiverId, currentUserId)
                .eq(UserChat::getSenderId, targetUserId)
                .eq(UserChat::getIsRead, ChatConstant.READ_STATUS_UNREAD)
                .set(UserChat::getIsRead, ChatConstant.READ_STATUS_READ)
                .set(UserChat::getUpdateTime, LocalDateTime.now()));

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
