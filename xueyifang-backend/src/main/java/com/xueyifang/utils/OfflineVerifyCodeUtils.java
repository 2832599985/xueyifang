package com.xueyifang.utils;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 校内线下交易动态核销引擎（无状态 TOTP / HMAC-SHA256）
 * <p>
 * 1. 动态 6 位数字核销码：每 30 秒自动轮换（兼容交接边界 8 秒宽限窗口），结合 Redis 暴力枚举限流。
 * 2. 动态防截图二维码令牌：每 10 秒自动轮换（仅容忍 3 秒网络传输宽限），格式 {@code XYF1:<orderId>:<slot>:<16-hex-hmac>}，
 *    配合前端纯实时摄像头流扫描，彻底杜绝远程发截图代核销。
 */
public final class OfflineVerifyCodeUtils {

    public static final long CODE_PERIOD_SECONDS = 30L;
    public static final long CODE_PREV_GRACE_SECONDS = 8L;

    public static final long QR_PERIOD_SECONDS = 10L;
    public static final long QR_PREV_GRACE_SECONDS = 3L;

    public static final String QR_VERSION_PREFIX = "XYF1";
    private static final String SEED_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int SEED_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OfflineVerifyCodeUtils() {
    }

    public enum QrVerifyStatus {
        VALID,
        EXPIRED,
        INVALID
    }

    public static final class ParsedQrToken {
        private final Long orderId;
        private final long timeSlot;
        private final String signature;

        public ParsedQrToken(Long orderId, long timeSlot, String signature) {
            this.orderId = orderId;
            this.timeSlot = timeSlot;
            this.signature = signature;
        }

        public Long getOrderId() {
            return orderId;
        }

        public long getTimeSlot() {
            return timeSlot;
        }

        public String getSignature() {
            return signature;
        }
    }

    /**
     * 为线下订单生成高熵 16 位基础密钥种子（存库，不直接暴露给前端）
     */
    public static String generateOrderSeed() {
        StringBuilder sb = new StringBuilder(SEED_LENGTH);
        for (int i = 0; i < SEED_LENGTH; i++) {
            sb.append(SEED_ALPHABET.charAt(SECURE_RANDOM.nextInt(SEED_ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * 计算指定时间窗口的 6 位动态数字核销码（30 秒周期，RFC 4226 动态截断）
     */
    public static String generateCode(Long orderId, String orderSeed, String serverSecret, long epochSeconds) {
        long timeSlot = Math.floorDiv(epochSeconds, CODE_PERIOD_SECONDS);
        return generateCodeForSlot(orderId, orderSeed, serverSecret, timeSlot);
    }

    /**
     * 校验 6 位动态数字核销码（允许当前 30s 窗口，以及新窗口前 8 秒内兼容上一窗口，避免读码跨秒边界失败）
     */
    public static boolean verifyCode(Long orderId, String orderSeed, String serverSecret,
                                     String inputCode, long epochSeconds) {
        if (orderId == null || StringUtils.isAnyBlank(orderSeed, inputCode)) {
            return false;
        }
        String cleanInput = inputCode.trim();
        if (!cleanInput.matches("^\\d{6}$")) {
            return false;
        }
        long currentSlot = Math.floorDiv(epochSeconds, CODE_PERIOD_SECONDS);
        long elapsedInSlot = Math.floorMod(epochSeconds, CODE_PERIOD_SECONDS);

        String currentExpected = generateCodeForSlot(orderId, orderSeed, serverSecret, currentSlot);
        if (constantTimeEquals(currentExpected, cleanInput)) {
            return true;
        }
        if (elapsedInSlot < CODE_PREV_GRACE_SECONDS) {
            String prevExpected = generateCodeForSlot(orderId, orderSeed, serverSecret, currentSlot - 1);
            return constantTimeEquals(prevExpected, cleanInput);
        }
        return false;
    }

    /**
     * 生成 10 秒刷新的签名动态二维码载荷：XYF1:<orderId>:<slot>:<16位HMAC摘要>
     */
    public static String generateQrToken(Long orderId, String orderSeed, String serverSecret, long epochSeconds) {
        long qrSlot = Math.floorDiv(epochSeconds, QR_PERIOD_SECONDS);
        String sig = computeQrSignature(orderId, orderSeed, serverSecret, qrSlot);
        return QR_VERSION_PREFIX + ":" + orderId + ":" + qrSlot + ":" + sig;
    }

    /**
     * 解析二维码载荷结构
     */
    public static ParsedQrToken parseQrToken(String qrToken) {
        if (StringUtils.isBlank(qrToken)) {
            return null;
        }
        String[] parts = qrToken.trim().split(":");
        if (parts.length != 4 || !QR_VERSION_PREFIX.equals(parts[0])) {
            return null;
        }
        try {
            Long orderId = Long.valueOf(parts[1]);
            long timeSlot = Long.parseLong(parts[2]);
            String sig = parts[3];
            if (orderId <= 0 || timeSlot <= 0 || sig.length() != 16) {
                return null;
            }
            return new ParsedQrToken(orderId, timeSlot, sig);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 校验 10 秒动态二维码（严格限制当前 10s 槽位 + 最多 3 秒传输宽限，超时明确返回 EXPIRED）
     */
    public static QrVerifyStatus verifyQrToken(Long expectedOrderId, String orderSeed, String serverSecret,
                                               ParsedQrToken parsed, long epochSeconds) {
        if (expectedOrderId == null || parsed == null || StringUtils.isBlank(orderSeed)) {
            return QrVerifyStatus.INVALID;
        }
        if (!expectedOrderId.equals(parsed.getOrderId())) {
            return QrVerifyStatus.INVALID;
        }
        String expectedSig = computeQrSignature(expectedOrderId, orderSeed, serverSecret, parsed.getTimeSlot());
        if (!constantTimeEquals(expectedSig, parsed.getSignature())) {
            return QrVerifyStatus.INVALID;
        }

        long currentSlot = Math.floorDiv(epochSeconds, QR_PERIOD_SECONDS);
        long elapsedInSlot = Math.floorMod(epochSeconds, QR_PERIOD_SECONDS);
        long slotDiff = currentSlot - parsed.getTimeSlot();

        if (slotDiff == 0) {
            return QrVerifyStatus.VALID;
        }
        if (slotDiff == 1 && elapsedInSlot < QR_PREV_GRACE_SECONDS) {
            return QrVerifyStatus.VALID;
        }
        if (slotDiff > 0) {
            return QrVerifyStatus.EXPIRED;
        }
        // 未来时间槽位非法
        return QrVerifyStatus.INVALID;
    }

    /**
     * 计算当前时间片距离下次刷新的剩余秒数（范围 1 ~ periodSeconds）
     */
    public static int remainingSeconds(long epochSeconds, long periodSeconds) {
        long rem = periodSeconds - Math.floorMod(epochSeconds, periodSeconds);
        return (int) rem;
    }

    private static String generateCodeForSlot(Long orderId, String orderSeed, String serverSecret, long timeSlot) {
        byte[] hmac = hmacSha256(buildSecretKey(serverSecret, orderSeed), "CODE:" + orderId + ":" + timeSlot);
        int offset = hmac[hmac.length - 1] & 0x0F;
        int binary = ((hmac[offset] & 0x7F) << 24)
                | ((hmac[offset + 1] & 0xFF) << 16)
                | ((hmac[offset + 2] & 0xFF) << 8)
                | (hmac[offset + 3] & 0xFF);
        int otp = binary % 1_000_000;
        return String.format("%06d", otp);
    }

    private static String computeQrSignature(Long orderId, String orderSeed, String serverSecret, long qrSlot) {
        byte[] hmac = hmacSha256(buildSecretKey(serverSecret, orderSeed), "QR:" + orderId + ":" + qrSlot);
        return toHex(hmac).substring(0, 16);
    }

    private static String buildSecretKey(String serverSecret, String orderSeed) {
        String master = StringUtils.isNotBlank(serverSecret) ? serverSecret : "xueyifang-default-verify-secret";
        return master + "::" + orderSeed;
    }

    private static byte[] hmacSha256(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 computation failed", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
