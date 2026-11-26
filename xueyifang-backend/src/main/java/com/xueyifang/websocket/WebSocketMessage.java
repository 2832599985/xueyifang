package com.xueyifang.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息体
 *
 * @author xueyifang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息数据
     */
    private Object data;

    // ==================== 消息类型常量 ====================

    /**
     * 新聊天消息
     */
    public static final String TYPE_NEW_CHAT = "NEW_CHAT";

    /**
     * 新通知
     */
    public static final String TYPE_NEW_NOTIFICATION = "NEW_NOTIFICATION";

    /**
     * 未读数更新
     */
    public static final String TYPE_UNREAD_UPDATE = "UNREAD_UPDATE";

    /**
     * 心跳响应
     */
    public static final String TYPE_PONG = "PONG";

    /**
     * 连接成功
     */
    public static final String TYPE_CONNECTED = "CONNECTED";

    // ==================== 工厂方法 ====================

    /**
     * 创建新聊天消息
     */
    public static WebSocketMessage newChat(Object chatData) {
        return WebSocketMessage.builder()
                .type(TYPE_NEW_CHAT)
                .data(chatData)
                .build();
    }

    /**
     * 创建新通知消息
     */
    public static WebSocketMessage newNotification(Object notificationData) {
        return WebSocketMessage.builder()
                .type(TYPE_NEW_NOTIFICATION)
                .data(notificationData)
                .build();
    }

    /**
     * 创建未读数更新消息
     */
    public static WebSocketMessage unreadUpdate(Object unreadData) {
        return WebSocketMessage.builder()
                .type(TYPE_UNREAD_UPDATE)
                .data(unreadData)
                .build();
    }

    /**
     * 创建心跳响应
     */
    public static WebSocketMessage pong() {
        return WebSocketMessage.builder()
                .type(TYPE_PONG)
                .data(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建连接成功消息
     */
    public static WebSocketMessage connected(Long userId) {
        return WebSocketMessage.builder()
                .type(TYPE_CONNECTED)
                .data(userId)
                .build();
    }
}
