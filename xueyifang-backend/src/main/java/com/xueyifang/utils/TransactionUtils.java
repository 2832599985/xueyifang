package com.xueyifang.utils;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务相关工具
 *
 * @author xueyifang
 */
public final class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * 在当前事务提交成功后执行任务；若当前不在事务中则立即执行。
     * <p>
     * 典型场景：WebSocket 推送、发送消息等"副作用"操作。
     * 如果在事务内直接推送，一旦事务回滚，用户就会收到一条实际并不存在的通知/消息。
     *
     * @param task 待执行任务
     */
    public static void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
