package com.xueyifang.config;

import com.xueyifang.websocket.WebSocketHandler;
import com.xueyifang.websocket.WebSocketHandshakeInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * WebSocket 配置类
 *
 * @author xueyifang
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${xueyifang.security.cors-allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOrigins;

    @Resource
    private WebSocketHandler webSocketHandler;

    @Resource
    private WebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] patterns = Arrays.stream(StringUtils.defaultIfBlank(allowedOrigins, "http://localhost:3000").split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
        registry.addHandler(webSocketHandler, "/ws")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(patterns);
    }
}
