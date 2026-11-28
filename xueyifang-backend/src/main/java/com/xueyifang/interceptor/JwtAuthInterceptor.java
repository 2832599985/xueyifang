package com.xueyifang.interceptor;

import com.xueyifang.common.ErrorCode;
import com.xueyifang.domain.po.User;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.JwtUtils;
import com.xueyifang.utils.RedisUtils;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

/**
 * JWT 认证拦截器
 * 
 * 功能：
 * 1. 从请求头获取 Token
 * 2. 验证 Token 有效性
 * 3. 检查 Token 是否在黑名单中（已被登出）
 * 4. 从 Token 获取用户ID，查询用户信息
 * 5. 将用户信息存入 ThreadLocal（UserContext）
 * 6. 请求结束后清理 ThreadLocal
 * 
 * 
 * @author xueyifang
 */
@Component
@Slf4j
public class JwtAuthInterceptor implements HandlerInterceptor {

    /**
     * 公开接口匹配：/professional/{id}, /service/{id}
     */
    private static final Pattern PROFESSIONAL_DETAIL_PATTERN = Pattern.compile("^/professional/\\d+$");
    private static final Pattern SERVICE_DETAIL_PATTERN = Pattern.compile("^/service/\\d+$");

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private IUserService userService;

    /**
     * 请求处理前执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {

        // 公开接口：不强制认证，但如果有合法 Token 则解析用户身份
        if (isPublicEndpoint(request)) {
            trySetUserContext(request);
            return true;
        }
        
        // 1. 从请求头获取 Token
        String token = getTokenFromRequest(request);
        
        if (StringUtils.isBlank(token)) {
            log.warn("请求未携带Token，路径：{}", request.getRequestURI());
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN, "请先登录");
        }
        
        // 2. 验证 Token 格式和有效性
        if (!jwtUtils.validateToken(token)) {
            log.warn("Token无效或已过期，路径：{}", request.getRequestURI());
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN, "登录已过期，请重新登录");
        }
        
        // 3. 检查 Token 是否在黑名单中（已被登出或踢下线）
        if (redisUtils.isTokenInBlacklist(token)) {
            log.warn("Token已失效（在黑名单中），路径：{}", request.getRequestURI());
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN, "账号已登出，请重新登录");
        }
        
        // 4. 从 Token 中获取用户ID
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.warn("Token中无法解析用户ID，路径：{}", request.getRequestURI());
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN, "登录信息异常，请重新登录");
        }

        // 4.5 检查 Token 是否因改密/重置密码/禁用账号而被吊销
        Long issuedAtMillis = jwtUtils.getTokenIssuedAtMillis(token);
        if (redisUtils.isTokenInvalidatedForUser(userId, issuedAtMillis)) {
            log.warn("Token已因安全变更失效，userId：{}，路径：{}", userId, request.getRequestURI());
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN, "登录凭证已失效，请重新登录");
        }
        
        // 5. 查询用户信息（优先从 Redis 会话缓存读取，未命中再查库并回填 30 分钟缓存）
        User user = loadUserWithCache(userId);
        if (user == null) {
            log.warn("用户不存在，userId：{}", userId);
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "用户不存在");
        }

        if (user.getIsDelete() != null && user.getIsDelete() == 1) {
            log.warn("用户已被删除，userId：{}", userId);
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "用户不存在或已被删除");
        }
        
        // 6. 检查账户状态
        if (user.getAccountStatus() != null && user.getAccountStatus() == 2) {
            log.warn("账户已被禁用，userId：{}", userId);
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账户已被禁用");
        }
        
        // 7. 将用户信息存入 ThreadLocal
        UserContext.setCurrentUser(user);
        
        // 8. 将Token存入Request，供logout接口使用
        request.setAttribute("currentToken", token);
        
        log.debug("用户认证成功，userId：{}，studentId：{}", userId, user.getStudentId());
        return true;
    }

    /**
     * 判断是否为公开接口（无需登录）
     */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.isNotBlank(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        if ("/professional/list".equals(uri)) {
            return true;
        }

        return PROFESSIONAL_DETAIL_PATTERN.matcher(uri).matches()
                || SERVICE_DETAIL_PATTERN.matcher(uri).matches();
    }

    /**
     * 尝试解析 Token 并设置 UserContext（不强制，失败静默忽略）
     * 用于公开接口的可选认证：有 Token 就识别身份，没有也不报错
     */
    private void trySetUserContext(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (StringUtils.isBlank(token)) {
            return;
        }
        try {
            if (!jwtUtils.validateToken(token) || redisUtils.isTokenInBlacklist(token)) {
                return;
            }
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                return;
            }
            if (redisUtils.isTokenInvalidatedForUser(userId, jwtUtils.getTokenIssuedAtMillis(token))) {
                return;
            }
            User user = loadUserWithCache(userId);
            if (user != null
                    && (user.getIsDelete() == null || user.getIsDelete() == 0)
                    && (user.getAccountStatus() == null || user.getAccountStatus() != 2)) {
                UserContext.setCurrentUser(user);
            }
        } catch (Exception e) {
            log.debug("公开接口可选认证失败（已忽略）: {}", e.getMessage());
        }
    }

    private User loadUserWithCache(Long userId) {
        User cached = redisUtils.getCachedUser(userId, User.class);
        if (cached != null) {
            return cached;
        }
        User dbUser = userService.getById(userId);
        if (dbUser != null && (dbUser.getIsDelete() == null || dbUser.getIsDelete() == 0)) {
            redisUtils.cacheUser(userId, dbUser, 1800L);
        }
        return dbUser;
    }

    /**
     * 请求处理后执行（视图渲染前）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, 
                                HttpServletResponse response, 
                                Object handler, 
                                Exception ex) {
        // ⚠️ 非常重要：清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
        log.debug("已清理 ThreadLocal");
    }

    /**
     * 从请求中提取 Token
     * 支持两种方式：
     * 1. Header: Authorization: Bearer {token}
     * 2. Header: token: {token}
     * 
     * @param request HTTP请求
     * @return Token字符串，如果没有返回null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 方式1：从 Authorization 头获取（推荐）
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 方式2：从 token 头获取（兼容）
        String token = request.getHeader("token");
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        
        // 方式3：从请求参数获取（不推荐，仅用于特殊场景如WebSocket）
        token = request.getParameter("token");
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        
        return null;
    }
}

