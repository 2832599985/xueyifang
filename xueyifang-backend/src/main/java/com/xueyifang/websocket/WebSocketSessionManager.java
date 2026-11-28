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
     * 移除用户会话（仅当当前注册会话与待关闭会话一致时移除，防止旧连接断开误删重连后的新会话）
     *
     * @param userId          用户ID
     * @param expectedSession 预期要移除的会话
     */
    public void removeSession(Long userId, WebSocketSession expectedSession) {
        if (userId == null) {
            return;
        }
        if (expectedSession == null) {
            removeSession(userId);
            return;
        }
        boolean removed = userSessions.remove(userId, expectedSession);
        if (!removed) {
            WebSocketSession current = userSessions.get(userId);
            if (current != null && current.getId().equals(expectedSession.getId())) {
                removed = userSessions.remove(userId, current);
            }
        }
        if (removed) {
            log.info("WebSocket连接断开: userId={}, sessionId={}, 当前在线人数={}",
                    userId, expectedSession.getId(), userSessions.size());
        }
    }

    /**
     * 移除用户会话
     *
     * @param userId 用户ID
     */
    public void removeSession(Long userId) {
        if (userId == null) {
            return;
        }
        userSessions.remove(userId);
        log.info("WebSocket连接断开: userId={}, 当前在线人数={}", userId, userSessions.size());
    }

    /**
     * 强制关闭并踢出指定用户的 WebSocket 会话（用于禁用账号、修改密码、重置密码等场景）
     *
     * @param userId 用户ID
     */
    public void closeSession(Long userId) {
        if (userId == null) {
            return;
        }
        WebSocketSession session = userSessions.remove(userId);
        if (session != null && session.isOpen()) {
            try {
                session.close();
                log.info("已强制断开用户WebSocket会话: userId={}", userId);
            } catch (IOException e) {
                log.warn("强制断开用户WebSocket会话失败: userId={}", userId, e);
            }
        }
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
            sendTextSafe(session, new TextMessage(jsonMessage));
            log.debug("WebSocket消息发送成功: userId={}, type={}", userId, message.getType());
            return true;
        } catch (IOException | IllegalStateException e) {
            log.error("WebSocket消息发送失败: userId={}", userId, e);
            // 发送失败，仅移除当前失效会话
            removeSession(userId, session);
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
                    sendTextSafe(session, textMessage);
                } catch (IOException | IllegalStateException e) {
                    log.error("广播消息发送失败: userId={}", userId, e);
                }
            }
        });
    }

    private void sendTextSafe(WebSocketSession session, TextMessage textMessage) throws IOException {
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
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
