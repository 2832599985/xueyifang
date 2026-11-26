package com.xueyifang.utils;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 低功耗蓝牙（BLE）近场碰面双向挑战-应答（Challenge-Response）加密引擎
 * <p>
 * 防御模型：
 * <ul>
 *   <li>防静态重放：每次会话由服务端签发一次性 {@code sessionId} 与买方挑战随机数 {@code buyerNonce}，核销后立即销毁</li>
 *   <li>防单端伪造：买方外围广播设备与卖方中心扫描设备需在近场通过 GATT 特征值交换 {@code buyerNonce} 与 {@code sellerNonce}</li>
 *   <li>防主密钥泄露：客户端仅获取单次会话派生的角色子密钥 {@code roleKey = HMAC(masterSecret, sessionId:orderId:role)}，永不接触订单种子</li>
 *   <li>防远程中继：结合客户端上报的 BLE RSSI 信号衰减值与双端实时 GPS 坐标差双重校验</li>
 * </ul>
 */
public final class BleMeetupCryptoUtils {

    public static final String ADV_PREFIX = "XYFB:";
    public static final String ROLE_BUYER = "BUYER";
    public static final String ROLE_SELLER = "SELLER";

    private static final String HEX_CHARS = "0123456789abcdef";
    private static final String TOKEN_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private BleMeetupCryptoUtils() {
    }

    /**
     * 生成 24 位高熵会话 ID
     */
    public static String generateSessionId() {
        return randomHex(12);
    }

    /**
     * 生成 16 位十六进制随机挑战 Nonce
     */
    public static String generateNonce() {
        return randomHex(8);
    }

    /**
     * 生成供 BLE 广播包（Manufacturer Specific Data / LocalName，受限于 31 字节广播帧）携带的短令牌
     * 格式：XYFB:<8位大写字母数字>，仅 13 字节，完美适配微信小程序与 Android BLE 广播包长度限制
     */
    public static String generateShortAdvToken() {
        StringBuilder sb = new StringBuilder(ADV_PREFIX.length() + 8);
        sb.append(ADV_PREFIX);
        for (int i = 0; i < 8; i++) {
            sb.append(TOKEN_ALPHABET.charAt(SECURE_RANDOM.nextInt(TOKEN_ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * 派生单次会话单角色签名子密钥（仅下发给已认证的对应买方或卖方客户端）
     */
    public static String deriveRoleSigningKey(String masterSecret, Long orderId, String sessionId, String role) {
        String material = "BLE_ROLE_KEY:" + orderId + ":" + sessionId + ":" + role;
        return hmacSha256Hex(masterSecret, material);
    }

    /**
     * 计算近场握手交叉签名
     * 载荷格式：XYF_BLE_V1|<orderId>|<sessionId>|<buyerNonce>|<sellerNonce>|<role>
     */
    public static String computeHandshakeSignature(String roleSigningKey,
                                                   Long orderId,
                                                   String sessionId,
                                                   String buyerNonce,
                                                   String sellerNonce,
                                                   String role) {
        String canonicalPayload = buildCanonicalPayload(orderId, sessionId, buyerNonce, sellerNonce, role);
        return hmacSha256Hex(roleSigningKey, canonicalPayload);
    }

    /**
     * 恒定时间验证客户端提交的 BLE 近场握手签名
     */
    public static boolean verifyHandshakeSignature(String masterSecret,
                                                   Long orderId,
                                                   String sessionId,
                                                   String buyerNonce,
                                                   String sellerNonce,
                                                   String role,
                                                   String clientSignature) {
        if (orderId == null || StringUtils.isAnyBlank(masterSecret, sessionId, buyerNonce, sellerNonce, role, clientSignature)) {
            return false;
        }
        String roleKey = deriveRoleSigningKey(masterSecret, orderId, sessionId, role);
        String expected = computeHandshakeSignature(roleKey, orderId, sessionId, buyerNonce, sellerNonce, role);
        return constantTimeEquals(expected, clientSignature.trim().toLowerCase());
    }

    public static String buildCanonicalPayload(Long orderId,
                                               String sessionId,
                                               String buyerNonce,
                                               String sellerNonce,
                                               String role) {
        return "XYF_BLE_V1|" + orderId + "|" + sessionId + "|" + buyerNonce + "|" + sellerNonce + "|" + role;
    }

    public static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 computation failed", e);
        }
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String randomHex(int byteLen) {
        byte[] bytes = new byte[byteLen];
        SECURE_RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX_CHARS.charAt(v >>> 4);
            out[i * 2 + 1] = HEX_CHARS.charAt(v & 0x0F);
        }
        return new String(out);
    }
}
