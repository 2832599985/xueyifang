package com.xueyifang.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket 消息处理器
 * 处理WebSocket连接、断开、消息接收等事件
 *
 * @author xueyifang
 */
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    @Resource
    private WebSocketSessionManager sessionManager;

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
     * 连接建立后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId == null) {
            log.warn("WebSocket连接建立失败: 无法获取用户ID");
            session.close();
            return;
        }

        // 注册会话
        sessionManager.addSession(userId, session);

        // 发送连接成功消息
        WebSocketMessage connectedMessage = WebSocketMessage.connected(userId);
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(gson.toJson(connectedMessage)));
            }
        }
    }

    /**
     * 收到消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getUserId(session);
        String payload = message.getPayload();

        try {
            JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
            String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : null;

            if (type == null) {
                log.warn("收到无效消息: 缺少type字段, userId={}", userId);
                return;
            }

            switch (type) {
                case "PING":
                    // 心跳请求，返回PONG
                    WebSocketMessage pongMessage = WebSocketMessage.pong();
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(gson.toJson(pongMessage)));
                        }
                    }
                    break;

                default:
                    log.debug("收到未处理的消息类型: type={}, userId={}", type, userId);
                    break;
            }

        } catch (Exception e) {
            log.error("处理WebSocket消息异常: userId={}", userId, e);
        }
    }

    /**
     * 连接关闭后
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            sessionManager.removeSession(userId, session);
        }
    }

    /**
     * 传输异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = getUserId(session);
        log.error("WebSocket传输异常: userId={}", userId, exception);

        if (session.isOpen()) {
            session.close();
        }

        if (userId != null) {
            sessionManager.removeSession(userId, session);
        }
    }

    /**
     * 从会话属性中获取用户ID
     */
    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(WebSocketHandshakeInterceptor.USER_ID_ATTR);
        return userId != null ? (Long) userId : null;
    }
}
