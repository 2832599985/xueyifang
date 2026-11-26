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
 * 订单自动取消定时任务
 * <p>
 * 每10分钟执行一次，自动取消长期未支付的订单（带 Redis 分布式锁保证多节点幂等单次调度）
 *
 * @author xueyifang
 */
@Component
public class OrderAutoCancelTask {

    private static final String LOCK_KEY = "lock:task:order_auto_cancel";
    private static final long LOCK_EXPIRE_SECONDS = 540L;

    @Resource
    private IServiceOrderService serviceOrderService;

    @Resource
    private RedisUtils redisUtils;

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCancelTask.class);

    @Scheduled(cron = "0 */10 * * * ?")
    public void autoCancelUnpaidOrders() {
        String token = UUID.randomUUID().toString();
        if (redisUtils != null && !redisUtils.tryLock(LOCK_KEY, token, LOCK_EXPIRE_SECONDS)) {
            log.debug("订单自动取消任务已在其他实例运行，本节点跳过");
            return;
        }
        log.info("开始执行订单自动取消任务");
        try {
            serviceOrderService.autoCancelUnpaidOrders();
            log.info("订单自动取消任务执行完成");
        } catch (Exception e) {
            log.error("订单自动取消任务执行失败", e);
        } finally {
            if (redisUtils != null) {
                redisUtils.unlock(LOCK_KEY, token);
            }
        }
    }
}
