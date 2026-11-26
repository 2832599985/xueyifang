package com.xueyifang.aop;

import com.xueyifang.annotation.RateLimit;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.domain.po.User;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 接口限流 AOP 切面（v3.0 生产级升级：基于 Redis ZSet + Lua 原子脚本的精确滑动时间窗限流）
 * <p>
 * 彻底修复原实现使用固定窗口 INCR 在时间窗交界处产生 2 倍突发流量穿透的漏洞。
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private static final String KEY_PREFIX = "ratelimit:sw:";

    private static final DefaultRedisScript<Long> SLIDING_WINDOW_LUA;

    static {
        SLIDING_WINDOW_LUA = new DefaultRedisScript<>();
        SLIDING_WINDOW_LUA.setResultType(Long.class);
        SLIDING_WINDOW_LUA.setScriptText(
                "local key = KEYS[1]\n"
                + "local now = tonumber(ARGV[1])\n"
                + "local windowMs = tonumber(ARGV[2])\n"
                + "local maxReq = tonumber(ARGV[3])\n"
                + "local member = ARGV[4]\n"
                + "redis.call('ZREMRANGEBYSCORE', key, 0, now - windowMs)\n"
                + "local current = redis.call('ZCARD', key)\n"
                + "if current >= maxReq then\n"
                + "    return 0\n"
                + "end\n"
                + "redis.call('ZADD', key, now, member)\n"
                + "redis.call('PEXPIRE', key, windowMs + 1000)\n"
                + "return 1\n"
        );
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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

    @Around("@annotation(rateLimit)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(joinPoint, rateLimit);
        int maxRequests = rateLimit.maxRequests();
        int windowSeconds = rateLimit.windowSeconds();
        long nowMs = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        String uniqueMember = nowMs + "-" + UUID.randomUUID().toString().substring(0, 8);

        Long allowed = 1L;
        if (stringRedisTemplate != null) {
            try {
                allowed = stringRedisTemplate.execute(
                        SLIDING_WINDOW_LUA,
                        Collections.singletonList(key),
                        String.valueOf(nowMs),
                        String.valueOf(windowMs),
                        String.valueOf(maxRequests),
                        uniqueMember
                );
            } catch (Exception e) {
                log.warn("Redis 滑动窗口限流异常降级放行: key={}", key, e);
                allowed = 1L;
            }
        }

        if (allowed != null && allowed == 0L) {
            log.warn("滑动窗口限流拦截: key={}, max={}/{}s", key, maxRequests, windowSeconds);
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    "请求过于频繁，请 " + windowSeconds + " 秒后再试");
        }

        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + ":" + signature.getName();

        String limitKey = rateLimit.key();
        if (StringUtils.isBlank(limitKey)) {
            limitKey = methodName;
        }

        String identifier;
        if (rateLimit.limitType() == RateLimit.LimitType.USER) {
            User currentUser = UserContext.getCurrentUser();
            if (currentUser != null) {
                identifier = "user:" + currentUser.getId();
            } else {
                identifier = "ip:" + getClientIp();
            }
        } else {
            identifier = "ip:" + getClientIp();
        }

        return KEY_PREFIX + limitKey + ":" + identifier;
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String remoteAddr = request.getRemoteAddr();

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

    private boolean isValidIp(String ip) {
        return StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip);
    }

    private boolean isTrustedProxy(String remoteAddr) {
        return trustedProxySet.contains(remoteAddr);
    }
}
