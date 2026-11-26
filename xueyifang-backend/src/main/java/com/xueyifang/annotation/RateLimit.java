package com.xueyifang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 *
 * <p>基于 Redis 滑动窗口算法实现接口限流，防止恶意请求攻击。
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li><b>登录接口</b>：防止暴力破解，限制每分钟尝试次数</li>
 *   <li><b>注册接口</b>：防止批量注册，限制IP请求频率</li>
 *   <li><b>公开列表</b>：防止爬虫，限制匿名用户访问频率</li>
 *   <li><b>验证码接口</b>：防止短信轰炸，限制发送频率</li>
 * </ul>
 *
 * <h3>实现原理：</h3>
 * <ol>
 *   <li>使用 Redis 存储请求计数</li>
 *   <li>基于滑动窗口算法，在指定时间窗口内限制请求次数</li>
 *   <li>Key格式：ratelimit:{limitKey}:{userId|ip}</li>
 *   <li>超过限制返回 429 状态码（请求过于频繁）</li>
 * </ol>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 示例1：登录接口，每分钟最多5次尝试
 * {@code @PostMapping("/login")}
 * {@code @RateLimit(key = "login", maxRequests = 5, windowSeconds = 60)}
 * public BaseResponse&lt;String&gt; login(@RequestBody LoginRequest request) {
 *     return authService.login(request);
 * }
 *
 * // 示例2：服务列表，每秒最多10次请求
 * {@code @GetMapping("/list")}
 * {@code @RateLimit(key = "service_list", maxRequests = 10, windowSeconds = 1)}
 * public BaseResponse&lt;Page&lt;ServiceVO&gt;&gt; listServices(...) {
 *     return serviceService.list(...);
 * }
 * </pre>
 *
 * @author xueyifang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 Key 标识
     * <p>
     * 用于区分不同接口的限流规则。
     * 完整 Key = ratelimit:{key}:{userId|ip}
     *
     * @return 限流 Key
     */
    String key() default "";

    /**
     * 时间窗口内最大请求次数
     * <p>
     * 默认值：10 次
     *
     * @return 最大请求次数
     */
    int maxRequests() default 10;

    /**
     * 时间窗口大小（秒）
     * <p>
     * 默认值：60 秒（1分钟）
     *
     * @return 时间窗口大小（秒）
     */
    int windowSeconds() default 60;

    /**
     * 限流维度
     * <p>
     * USER：按用户ID限流（需要登录）<br>
     * IP：按IP地址限流（无需登录）<br>
     * <p>
     * 默认值：IP（适用于公开接口）
     *
     * @return 限流维度
     */
    LimitType limitType() default LimitType.IP;

    /**
     * 限流维度枚举
     */
    enum LimitType {
        /**
         * 按用户ID限流
         */
        USER,
        /**
         * 按IP地址限流
         */
        IP
    }
}
