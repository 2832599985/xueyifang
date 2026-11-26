package com.xueyifang.aop;

import com.xueyifang.annotation.RateLimit;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.domain.po.User;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.utils.RedisUtils;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 接口限流 AOP 切面
 * <p>
 * 基于 Redis 滑动窗口算法实现接口限流，保护系统不被恶意请求攻击。
 *
 * <h3>算法说明：</h3>
 * <ul>
 *   <li>使用 Redis INCR 原子操作计数</li>
 *   <li>每个时间窗口使用独立的 Key</li>
 *   <li>Key 过期时间 = 窗口大小，自动清理</li>
 * </ul>
 *
 * @author xueyifang
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private static final String KEY_PREFIX = "ratelimit:";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 受信任的代理IP（逗号分隔）。仅当请求来自这些代理时，才信任 X-Forwarded-For。
     */
    @Value("${security.trusted-proxies:}")
    private String trustedProxies;

    private Set<String> trustedProxySet = Collections.emptySet();

    @PostConstruct
    private void initTrustedProxies() {
        if (StringUtils.isBlank(trustedProxies)) {
            return;
        }
        trustedProxySet = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (!trustedProxySet.isEmpty()) {
            log.info("已配置受信任代理IP: {}", trustedProxySet);
        }
    }

    /**
     * 限流检查拦截
     *
     * @param joinPoint 切入点
     * @param rateLimit 限流注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(rateLimit)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 构建限流 Key
        String key = buildKey(joinPoint, rateLimit);

        // 获取限流参数
        int maxRequests = rateLimit.maxRequests();
        int windowSeconds = rateLimit.windowSeconds();

        // 原子递增计数
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            // Redis 异常，放行
            log.warn("限流检查异常: key={}", key);
            return joinPoint.proceed();
        }

        if (count == 1) {
            // 第一次请求，设置过期时间
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        if (count > maxRequests) {
            // 超过限流阈值
            log.warn("接口限流拦截: key={}, count={}, max={}", key, count, maxRequests);
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    "请求过于频繁，请 " + windowSeconds + " 秒后再试");
        }

        // 未超过限制，放行
        log.debug("限流检查通过: key={}, count={}/{}", key, count, maxRequests);
        return joinPoint.proceed();
    }

    /**
     * 构建限流 Key
     *
     * @param joinPoint 切入点
     * @param rateLimit 限流注解
     * @return Redis Key
     */
    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + ":" + signature.getName();

        // 获取限流 Key 标识
        String limitKey = rateLimit.key();
        if (StringUtils.isBlank(limitKey)) {
            limitKey = methodName;
        }

        // 获取限流维度标识
        String identifier;
        if (rateLimit.limitType() == RateLimit.LimitType.USER) {
            // 按用户限流
            User currentUser = UserContext.getCurrentUser();
            if (currentUser != null) {
                identifier = "user:" + currentUser.getId();
            } else {
                // 未登录用户降级为 IP 限流
                identifier = "ip:" + getClientIp();
            }
        } else {
            // 按 IP 限流
            identifier = "ip:" + getClientIp();
        }

        return KEY_PREFIX + limitKey + ":" + identifier;
    }

    /**
     * 获取客户端 IP 地址
     * <p>
     * 支持代理服务器场景，优先从 X-Forwarded-For 等头获取
     *
     * @return IP 地址
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();

        String remoteAddr = request.getRemoteAddr();

        // 仅当来自受信任代理时，才解析转发头
        if (isTrustedProxy(remoteAddr)) {
            String ip = request.getHeader("X-Forwarded-For");
            if (isValidIp(ip)) {
                int index = ip.indexOf(',');
                if (index > 0) {
                    ip = ip.substring(0, index);
                }
                return ip.trim();
            }

            ip = request.getHeader("X-Real-IP");
            if (isValidIp(ip)) {
                return ip;
            }

            ip = request.getHeader("Proxy-Client-IP");
            if (isValidIp(ip)) {
                return ip;
            }

            ip = request.getHeader("WL-Proxy-Client-IP");
            if (isValidIp(ip)) {
                return ip;
            }
        }

        return remoteAddr;
    }

    /**
     * 判断 IP 是否有效
     */
    private boolean isValidIp(String ip) {
        return StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip);
    }

    private boolean isTrustedProxy(String remoteAddr) {
        return trustedProxySet.contains(remoteAddr);
    }
}
