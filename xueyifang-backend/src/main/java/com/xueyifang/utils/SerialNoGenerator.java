package com.xueyifang.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务流水号生成器
 * <p>
 * 格式：前缀 + yyyyMMddHHmmssSSS + [业务ID] + 3位自增序列 + 2位随机数
 * <ul>
 *   <li>毫秒级时间戳 + JVM 内自增序列，保证单实例同一毫秒内也不会重复</li>
 *   <li>追加随机数，降低多实例部署时的碰撞概率</li>
 * </ul>
 * 旧实现（秒级时间戳 + userId）在同一事务内连续写两条流水时必然重复，已废弃。
 *
 * @author xueyifang
 */
public final class SerialNoGenerator {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private static final String ORDER_PREFIX = "ORDER";

    private static final String TRANSACTION_PREFIX = "WT";

    private SerialNoGenerator() {
    }

    /**
     * 生成订单编号，例如：ORDER2026112319253012300142
     */
    public static String nextOrderNo() {
        return build(ORDER_PREFIX, null);
    }

    /**
     * 生成钱包交易流水号，例如：WT20261123192530123 + userId + 00142
     */
    public static String nextTransactionNo(Long userId) {
        return build(TRANSACTION_PREFIX, userId);
    }

    private static String build(String prefix, Long bizId) {
        int seq = Math.floorMod(SEQUENCE.getAndIncrement(), 1000);
        int random = ThreadLocalRandom.current().nextInt(100);
        StringBuilder sb = new StringBuilder(40)
                .append(prefix)
                .append(LocalDateTime.now().format(TIME_FORMATTER));
        if (bizId != null) {
            sb.append(bizId);
        }
        return sb.append(String.format("%03d%02d", seq, random)).toString();
    }
}
