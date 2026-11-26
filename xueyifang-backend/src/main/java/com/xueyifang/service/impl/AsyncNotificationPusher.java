package com.xueyifang.service.impl;

import com.xueyifang.domain.po.Notification;
import com.xueyifang.domain.vo.NotificationVO;
import com.xueyifang.websocket.WebSocketMessage;
import com.xueyifang.websocket.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 异步通知推送服务
 *
 * <p>将 WebSocket 通知推送操作异步化，不阻塞主业务流程。
 *
 * <h3>设计原因：</h3>
 * <ul>
 *   <li>Spring @Async 需要通过代理调用才能生效</li>
 *   <li>同一类内部调用不会触发 AOP 代理</li>
 *   <li>因此将异步推送抽离为独立服务</li>
 * </ul>
 *
 * @author xueyifang
 */
@Slf4j
@Service
public class AsyncNotificationPusher {

    @Resource
    private WebSocketSessionManager webSocketSessionManager;

    /**
     * 异步推送通知给用户
     * <p>
     * 通过 WebSocket 推送通知，失败不影响主流程
     *
     * @param notification 通知实体
     */
    @Async("asyncTaskExecutor")
    public void pushNotificationAsync(Notification notification) {
        try {
            log.debug("异步推送通知: recipientId={}, type={}",
                    notification.getRecipientId(), notification.getNotificationType());

            NotificationVO vo = new NotificationVO();
            BeanUtils.copyProperties(notification, vo);
            WebSocketMessage wsMessage = WebSocketMessage.newNotification(vo);
            webSocketSessionManager.sendToUser(notification.getRecipientId(), wsMessage);

            log.debug("异步推送通知成功: recipientId={}", notification.getRecipientId());
        } catch (Exception e) {
            // 异步推送失败仅记录日志，不影响业务
            log.warn("异步推送通知失败: recipientId={}, error={}",
                    notification.getRecipientId(), e.getMessage());
        }
    }

    /**
     * 异步发送 WebSocket 消息
     * <p>
     * 通用的异步 WebSocket 消息发送方法
     *
     * @param userId 目标用户ID
     * @param message WebSocket 消息
     */
    @Async("asyncTaskExecutor")
    public void sendMessageAsync(Long userId, WebSocketMessage message) {
        try {
            log.debug("异步发送 WebSocket 消息: userId={}, type={}", userId, message.getType());
            webSocketSessionManager.sendToUser(userId, message);
            log.debug("异步发送 WebSocket 消息成功: userId={}", userId);
        } catch (Exception e) {
            log.warn("异步发送 WebSocket 消息失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
