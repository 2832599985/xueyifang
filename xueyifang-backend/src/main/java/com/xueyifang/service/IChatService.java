package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.domain.dto.chat.ChatSendRequest;
import com.xueyifang.domain.po.UserChat;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.vo.ChatConversationVO;
import com.xueyifang.domain.vo.ChatMessageVO;

import java.util.List;

/**
 * <p>
 * 用户聊天记录表 服务类
 * </p>
 *
 * @author xueyifang
 * @since 2025-11-24
 */
public interface IChatService extends IService<UserChat> {

    /**
     * 发送聊天消息
     *
     * @param request 发送消息请求
     */
    void sendMessage(ChatSendRequest request);

    /**
     * 获取与指定用户的聊天记录
     *
     * @param targetUserId 对方用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 聊天消息分页
     */
    Page<ChatMessageVO> getChatMessagesWithUser(Long targetUserId, Integer pageNum, Integer pageSize);

    /**
     * 获取我的聊天会话列表
     *
     * @return 会话列表
     */
    List<ChatConversationVO> getMyConversations();
}
