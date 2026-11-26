package com.xueyifang.aop;

import com.xueyifang.annotation.IdempotentSubmit;
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
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 防重复提交 AOP 切面
 * <p>
 * 基于 Redis 实现幂等性检查，防止用户短时间内重复提交请求。
 *
 * <h3>实现机制：</h3>
 * <ul>
 *   <li>使用 Redis SETNX（SET if Not eXists）原子操作</li>
 *   <li>Key格式：idempotent:{userId}:{className}:{methodName}[:firstParam]</li>
 *   <li>Value为时间戳，用于调试</li>
 *   <li>设置过期时间，自动释放锁</li>
 * </ul>
 *
 * @author xueyifang
 */
@Aspect
@Component
@Slf4j
public class IdempotentAspect {

    private static final String KEY_PREFIX = "idempotent:";

    @Resource
    private RedisUtils redisUtils;

    /**
     * 幂等性检查拦截
     *
     * @param joinPoint 切入点
     * @param idempotentSubmit 幂等注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(idempotentSubmit)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, IdempotentSubmit idempotentSubmit) throws Throwable {
        // 获取当前登录用户
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            // 未登录用户不做幂等检查，交给后续权限校验处理
            return joinPoint.proceed();
        }

        // 构建 Redis Key
        String key = buildKey(joinPoint, currentUser.getId(), idempotentSubmit.useFirstParam());

        // 获取锁定时间
        int lockTime = idempotentSubmit.lockTime();
        if (lockTime <= 0) {
            lockTime = 3; // 默认3秒
        }

        // 尝试获取锁（原子操作）
        boolean acquired = redisUtils.setIfAbsent(key, System.currentTimeMillis(), lockTime);

        if (!acquired) {
            // 获取锁失败，说明正在处理中或刚刚处理完毕
            log.warn("防重复提交拦截: userId={}, key={}", currentUser.getId(), key);

            // 获取自定义错误消息
            String message = idempotentSubmit.message();
            if (StringUtils.isBlank(message)) {
                message = ErrorCode.DUPLICATE_SUBMIT.getMessage();
            }

            throw new BusinessException(ErrorCode.DUPLICATE_SUBMIT, message);
        }

        // 获取锁成功，执行业务方法
        log.debug("防重复提交通过: userId={}, key={}, lockTime={}s", currentUser.getId(), key, lockTime);
        return joinPoint.proceed();
    }

    /**
     * 构建 Redis Key
     *
     * @param joinPoint 切入点
     * @param userId 用户ID
     * @param useFirstParam 是否使用第一个参数
     * @return Redis Key
     */
    private String buildKey(ProceedingJoinPoint joinPoint, Long userId, boolean useFirstParam) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        StringBuilder keyBuilder = new StringBuilder(KEY_PREFIX)
                .append(userId).append(":")
                .append(className).append(":")
                .append(methodName);

        // 如果需要使用第一个参数
        if (useFirstParam) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0 && args[0] != null) {
                keyBuilder.append(":").append(args[0].toString());
            }
        }

        return keyBuilder.toString();
    }
}
