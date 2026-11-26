package com.xueyifang.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步处理配置
 *
 * <p>配置 Spring 异步任务执行器，用于非核心操作的异步执行。
 *
 * <h3>适用场景：</h3>
 * <ul>
 *   <li>通知消息推送</li>
 *   <li>WebSocket 消息发送</li>
 *   <li>操作日志记录</li>
 *   <li>邮件/短信发送</li>
 * </ul>
 *
 * <h3>线程池参数说明：</h3>
 * <ul>
 *   <li>corePoolSize=4：核心线程数，适合小型项目</li>
 *   <li>maxPoolSize=8：最大线程数</li>
 *   <li>queueCapacity=100：任务队列容量</li>
 *   <li>keepAliveSeconds=60：非核心线程空闲存活时间</li>
 *   <li>CallerRunsPolicy：队列满时由调用线程执行，保证任务不丢失</li>
 * </ul>
 *
 * @author xueyifang
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 默认异步任务执行器
     */
    @Override
    @Bean("asyncTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(4);
        // 最大线程数
        executor.setMaxPoolSize(8);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名前缀
        executor.setThreadNamePrefix("async-");
        // 空闲线程存活时间
        executor.setKeepAliveSeconds(60);
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("异步任务执行器初始化完成: corePoolSize=4, maxPoolSize=8, queueCapacity=100");
        return executor;
    }

    /**
     * 异步任务异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            log.error("异步任务执行异常: method={}, params={}", method.getName(), objects, throwable);
        };
    }
}
