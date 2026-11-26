package com.xueyifang.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务流水号生成器单元测试
 */
class SerialNoGeneratorTest {

    @Test
    void nextOrderNo_hasExpectedFormatAndUniqueness() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            String no = SerialNoGenerator.nextOrderNo();
            assertTrue(no.matches("^ORDER\\d{22}$"), "订单号格式异常: " + no);
            generated.add(no);
        }
        assertEquals(2000, generated.size(), "同一毫秒连续生成的订单号不能重复");
    }

    @Test
    void nextTransactionNo_sameUserSameMillisecond_isUnique() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            String no = SerialNoGenerator.nextTransactionNo(42L);
            assertTrue(no.matches("^WT\\d{17}42\\d{5}$"), "流水号格式异常: " + no);
            generated.add(no);
        }
        assertEquals(2000, generated.size(), "同用户同事务内连续生成的流水号不能重复");
    }

    @Test
    void nextTransactionNo_concurrentCalls_noCollisions() throws InterruptedException {
        int threads = 8;
        int perThread = 500;
        Set<String> generated = Collections.synchronizedSet(new HashSet<>());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        generated.add(SerialNoGenerator.nextTransactionNo(1L));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();
        assertEquals(threads * perThread, generated.size());
    }
}
