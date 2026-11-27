package com.xueyifang.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务流水号生成器（支持订单、钱包、悬赏、第三方担保支付单与平台抽佣台账单号）
 */
public final class SerialNoGenerator {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private static final String ORDER_PREFIX = "ORDER";
    private static final String TRANSACTION_PREFIX = "WT";
    private static final String PAYMENT_PREFIX = "PAY";
    private static final String REVENUE_PREFIX = "REV";
    private static final String DEMAND_PREFIX = "DEMAND";

    private SerialNoGenerator() {
    }

    public static String nextOrderNo() {
        return build(ORDER_PREFIX, null);
    }

    public static String nextDemandNo() {
        return build(DEMAND_PREFIX, null);
    }

    public static String next(String prefix) {
        return build(prefix != null ? prefix : "NO", null);
    }

    public static String nextTransactionNo(Long userId) {
        return build(TRANSACTION_PREFIX, userId);
    }

    public static String nextPaymentNo(Long orderId) {
        return build(PAYMENT_PREFIX, orderId);
    }

    public static String nextRevenueLedgerNo(Long orderId) {
        return build(REVENUE_PREFIX, orderId);
    }

    private static String build(String prefix, Long bizId) {
        int seq = Math.floorMod(SEQUENCE.getAndIncrement(), 1000);
        int random = ThreadLocalRandom.current().nextInt(100);
        StringBuilder sb = new StringBuilder(44)
                .append(prefix)
                .append(LocalDateTime.now().format(TIME_FORMATTER));
        if (bizId != null) {
            sb.append(bizId);
        }
        return sb.append(String.format("%03d%02d", seq, random)).toString();
    }
}
