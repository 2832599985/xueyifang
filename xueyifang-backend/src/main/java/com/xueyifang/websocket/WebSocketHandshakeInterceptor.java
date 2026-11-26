package com.xueyifang.websocket;

import com.xueyifang.utils.JwtUtils;
import com.xueyifang.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 用于在WebSocket连接建立前验证JWT Token
 *
 * @author xueyifang
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private RedisUtils redisUtils;

    /**
     * Token黑名单前缀（与 JwtAuthInterceptor 保持一致）
     */
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * WebSocket会话属性：用户ID
     */
    public static final String USER_ID_ATTR = "userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 从URL参数获取token: /ws?token=xxx
        String token = null;

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            token = servletRequest.getServletRequest().getParameter("token");
        }

        // 如果URL参数没有token，尝试从查询字符串解析
        if (token == null || token.isEmpty()) {
            String query = request.getURI().getQuery();
            if (query != null && query.contains("token=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        token = param.substring(6);
                        break;
                    }
                }
            }
        }

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket握手失败: 缺少token");
            return false;
        }

        try {
            // 验证token有效性
            if (!jwtUtils.validateToken(token)) {
                log.warn("WebSocket握手失败: token无效");
                return false;
            }

            // 检查token是否在黑名单中（已登出）
            if (redisUtils.isTokenInBlacklist(token)) {
                log.warn("WebSocket握手失败: token已登出");
                return false;
            }

            // 获取用户ID
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("WebSocket握手失败: 无法从token获取用户ID");
                return false;
            }

            // 将用户ID存入WebSocket会话属性
            attributes.put(USER_ID_ATTR, userId);
            log.debug("WebSocket握手成功: userId={}", userId);
            return true;

        } catch (Exception e) {
            log.error("WebSocket握手异常", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后的处理（如有需要）
    }
}
