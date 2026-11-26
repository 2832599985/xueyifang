package com.xueyifang.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器
 * 管理用户ID与WebSocket会话的映射关系
 *
 * @author xueyifang
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * 用户ID -> WebSocket会话 映射
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /**
     * 配置 Gson 支持 LocalDateTime 序列化
     */
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                @Override
                public void write(JsonWriter out, LocalDateTime value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.format(formatter));
                    }
                }

                @Override
                public LocalDateTime read(JsonReader in) throws IOException {
                    String value = in.nextString();
                    if (value == null || value.isEmpty()) {
                        return null;
                    }
                    return LocalDateTime.parse(value, formatter);
                }
            })
            .create();

    /**
     * 添加用户会话
     *
     * @param userId  用户ID
     * @param session WebSocket会话
     */
    public void addSession(Long userId, WebSocketSession session) {
        // 如果用户已有旧会话，先关闭
        WebSocketSession oldSession = userSessions.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
            } catch (IOException e) {
                log.warn("关闭旧会话失败: userId={}", userId, e);
            }
        }
        userSessions.put(userId, session);
        log.info("WebSocket连接建立: userId={}, 当前在线人数={}", userId, userSessions.size());
    }

    /**
     * 移除用户会话
     *
     * @param userId 用户ID
     */
    public void removeSession(Long userId) {
        userSessions.remove(userId);
        log.info("WebSocket连接断开: userId={}, 当前在线人数={}", userId, userSessions.size());
    }

    /**
     * 获取用户会话
     *
     * @param userId 用户ID
     * @return WebSocket会话，不存在则返回null
     */
    public WebSocketSession getSession(Long userId) {
        return userSessions.get(userId);
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return true=在线，false=不在线
     */
    public boolean isOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId  目标用户ID
     * @param message 消息对象
     * @return true=发送成功，false=用户不在线或发送失败
     */
    public boolean sendToUser(Long userId, WebSocketMessage message) {
        WebSocketSession session = userSessions.get(userId);
        if (session == null || !session.isOpen()) {
            log.debug("用户不在线，无法发送WebSocket消息: userId={}", userId);
            return false;
        }

        try {
            String jsonMessage = gson.toJson(message);
            session.sendMessage(new TextMessage(jsonMessage));
            log.debug("WebSocket消息发送成功: userId={}, type={}", userId, message.getType());
            return true;
        } catch (IOException e) {
            log.error("WebSocket消息发送失败: userId={}", userId, e);
            // 发送失败，移除无效会话
            removeSession(userId);
            return false;
        }
    }

    /**
     * 广播消息给所有在线用户
     *
     * @param message 消息对象
     */
    public void broadcast(WebSocketMessage message) {
        String jsonMessage = gson.toJson(message);
        TextMessage textMessage = new TextMessage(jsonMessage);

        userSessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("广播消息发送失败: userId={}", userId, e);
                }
            }
        });
    }

    /**
     * 获取当前在线人数
     *
     * @return 在线人数
     */
    public int getOnlineCount() {
        return userSessions.size();
    }
}
