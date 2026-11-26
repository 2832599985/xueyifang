package com.xueyifang.aop;

import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.po.User;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 权限校验 AOP 拦截器
 * <p>
 * 处理 @AuthCheck 注解的权限校验逻辑：
 * <ul>
 *   <li>角色校验：检查用户角色是否满足要求（如管理员角色）</li>
 *   <li>发布权限校验：检查用户是否拥有发布权限</li>
 * </ul>
 *
 * @author xueyifang
 */
@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    /**
     * 执行权限校验拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取当前登录用户
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        // 获取注解参数
        int requiredRole = authCheck.role();
        boolean needPublishPermission = authCheck.needPublishPermission();

        // 校验角色权限
        if (requiredRole != 0) {
            Integer userRole = currentUser.getRole();
            if (userRole == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户角色信息异常");
            }

            // 管理员角色校验
            if (requiredRole == UserConstant.ROLE_ADMIN && userRole != UserConstant.ROLE_ADMIN) {
                log.warn("权限校验失败：用户 {} 尝试访问管理员接口", currentUser.getId());
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
            }

            // 其他角色校验（如需要特定角色）
            if (requiredRole != UserConstant.ROLE_ADMIN && userRole != requiredRole) {
                log.warn("权限校验失败：用户 {} 角色不匹配，需要角色：{}，实际角色：{}",
                        currentUser.getId(), requiredRole, userRole);
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "角色权限不足");
            }
        }

        // 校验发布权限
        if (needPublishPermission) {
            Integer publishPermission = currentUser.getPublishPermission();
            if (publishPermission == null || publishPermission != UserConstant.HAS_PUBLISH_PERMISSION) {
                log.warn("权限校验失败：用户 {} 无发布权限", currentUser.getId());
                throw new BusinessException(ErrorCode.USER_NO_PUBLISH_PERMISSION);
            }
        }

        // 权限校验通过，执行原方法
        return joinPoint.proceed();
    }
}
