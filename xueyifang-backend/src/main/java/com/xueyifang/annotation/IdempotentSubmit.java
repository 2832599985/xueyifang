package com.xueyifang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解
 *
 * <p>基于 Redis + 用户ID + 方法名 实现幂等性检查，防止用户短时间内重复提交。
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li><b>支付场景</b>：防止用户多次点击支付按钮导致重复扣款</li>
 *   <li><b>收藏场景</b>：防止短时间内重复收藏同一服务</li>
 *   <li><b>权限申请</b>：防止重复提交权限申请</li>
 *   <li><b>订单创建</b>：防止重复创建订单</li>
 * </ul>
 *
 * <h3>实现原理：</h3>
 * <ol>
 *   <li>基于 Redis SETNX 原子操作</li>
 *   <li>Key格式：idempotent:{userId}:{methodName}[:param]</li>
 *   <li>在锁定时间内，相同用户对同一方法的重复请求将被拒绝</li>
 *   <li>锁定时间到期后自动释放，允许下次请求</li>
 * </ol>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 示例1：支付订单，3秒内防重复
 * {@code @PostMapping("/pay")}
 * {@code @IdempotentSubmit(lockTime = 3)}
 * public BaseResponse&lt;Void&gt; payOrder(@RequestBody PayRequest request) {
 *     return orderService.pay(request);
 * }
 *
 * // 示例2：收藏服务，使用第一个参数（serviceId）区分
 * {@code @PostMapping("/collect/{serviceId}")}
 * {@code @IdempotentSubmit(lockTime = 2, useFirstParam = true)}
 * public BaseResponse&lt;Void&gt; collectService(@PathVariable Long serviceId) {
 *     return favoriteService.collect(serviceId);
 * }
 * </pre>
 *
 * @author xueyifang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentSubmit {

    /**
     * 锁定时间（秒）
     * <p>
     * 在锁定时间内，相同用户对同一方法的重复请求将被拒绝。
     * <p>
     * 默认值：3秒，适用于大多数表单提交场景
     *
     * @return 锁定时间（秒）
     */
    int lockTime() default 3;

    /**
     * 是否使用第一个参数作为幂等 Key 的一部分
     * <p>
     * true：Key = idempotent:{userId}:{methodName}:{firstParam}<br>
     * false：Key = idempotent:{userId}:{methodName}
     * <p>
     * 使用场景：当同一个接口需要区分不同资源时（如收藏不同的服务）
     *
     * @return 是否使用第一个参数
     */
    boolean useFirstParam() default false;

    /**
     * 自定义错误提示消息
     * <p>
     * 如果为空，使用默认消息 "请勿重复提交"
     *
     * @return 错误提示消息
     */
    String message() default "";
}
