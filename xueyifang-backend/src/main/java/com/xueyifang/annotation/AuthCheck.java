package com.xueyifang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 *
 * <p>用于方法级别的权限控制，支持角色权限和发布权限的校验。
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li><b>发布服务权限控制</b>：用户发布服务时，需要检查是否拥有发布权限（publishPermission=1）
 *       <br>默认情况下，用户注册后可以直接发布服务。
 *       <br>但管理员可以修改系统设置，开启"发布权限审核"功能。
 *       <br>开启后，用户注册完需要先申请发布权限，由管理员审核通过后才能发布服务。
 *   </li>
 *   <li><b>管理员功能控制</b>：某些管理功能（如审核权限、删除用户等）只允许管理员访问</li>
 *   <li><b>组合权限控制</b>：同时校验角色和发布权限</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 示例1：发布服务 - 需要发布权限
 * {@code @PostMapping("/service/publish")}
 * {@code @AuthCheck(needPublishPermission = true)}
 * public BaseResponse&lt;Void&gt; publishService(@RequestBody ServiceRequest request) {
 *     // 只有 publishPermission=1 的用户才能访问
 *     return serviceService.publishService(request);
 * }
 *
 * // 示例2：管理员审核权限 - 需要管理员角色
 * {@code @PostMapping("/admin/review")}
 * {@code @AuthCheck(role = 2)}
 * public BaseResponse&lt;Void&gt; reviewPermission(@RequestBody ReviewRequest request) {
 *     // 只有管理员（role=2）才能访问
 *     return userService.reviewPermission(request);
 * }
 * </pre>
 *
 * <h3>业务流程：</h3>
 * <ol>
 *   <li>用户注册后，默认 publishPermission=0（无发布权限）</li>
 *   <li>如果系统设置为"需要审核"，用户需要申请发布权限</li>
 *   <li>管理员审核通过后，用户的 publishPermission 变为 1</li>
 *   <li>用户访问带有 {@code @AuthCheck(needPublishPermission = true)} 的接口时，会自动校验权限</li>
 *   <li>如果权限不足，抛出 BusinessException(ErrorCode.NO_AUTH_ERROR)</li>
 * </ol>
 *
 * @author xueyifang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须有某个角色
     * <p>
     * 0 = 不限制角色（默认）<br>
     * 1 = 学生角色<br>
     * 2 = 管理员角色
     *
     * @return 角色值
     */
    int role() default 0;

    /**
     * 是否需要发布权限
     * <p>
     * false = 不需要发布权限（默认）<br>
     * true = 需要发布权限（用户的 publishPermission 必须为 1）
     * <p>
     * <b>使用场景：</b>发布服务、修改服务等需要发布权限的操作
     *
     * @return 是否需要发布权限
     */
    boolean needPublishPermission() default false;
}

