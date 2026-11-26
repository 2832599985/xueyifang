package com.xueyifang.task;

import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * 订单自动确认收货定时任务
 *
 * 每小时执行一次，处理超时未确认收货的订单（带 Redis 分布式锁保证多节点单次调度）
 *
 * @author xueyifang
 * @date 2025-11-28
 */
@Component
public class OrderAutoConfirmReceiptTask {

    private static final String LOCK_KEY = "lock:task:order_auto_confirm_receipt";
    private static final long LOCK_EXPIRE_SECONDS = 3300L;

    @Resource
    private IServiceOrderService serviceOrderService;

    @Resource
    private RedisUtils redisUtils;

    private static final Logger log = LoggerFactory.getLogger(OrderAutoConfirmReceiptTask.class);

    @Scheduled(cron = "0 0 * * * ?")
    public void autoConfirmReceipt() {
        String token = UUID.randomUUID().toString();
        if (redisUtils != null && !redisUtils.tryLock(LOCK_KEY, token, LOCK_EXPIRE_SECONDS)) {
            log.debug("订单自动确认收货任务已在其他实例运行，本节点跳过");
            return;
        }
        log.info("开始执行订单自动确认收货任务");
        try {
            serviceOrderService.autoConfirmReceipt();
            log.info("订单自动确认收货任务执行完成");
        } catch (Exception e) {
            log.error("订单自动确认收货任务执行失败", e);
        } finally {
            if (redisUtils != null) {
                redisUtils.unlock(LOCK_KEY, token);
            }
        }
    }
}
