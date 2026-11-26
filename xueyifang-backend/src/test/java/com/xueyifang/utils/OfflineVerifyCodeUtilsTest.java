package com.xueyifang.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 动态 30s 核销码与 10s 防截图二维码加密引擎单元测试
 */
class OfflineVerifyCodeUtilsTest {

    private static final String SECRET = "test-master-secret-2026";
    private static final String SEED = "A7K9M3P5Q2W8X4Z6";

    @Test
    void generateCode_rotatesEvery30SecondsAndSupportsBoundaryGrace() {
        long base = 1795570200L; // 正好被 30 整除的时间槽起点
        String codeSlot0 = OfflineVerifyCodeUtils.generateCode(101L, SEED, SECRET, base + 12);
        String codeSlot0End = OfflineVerifyCodeUtils.generateCode(101L, SEED, SECRET, base + 29);
        String codeSlot1 = OfflineVerifyCodeUtils.generateCode(101L, SEED, SECRET, base + 30);

        assertTrue(codeSlot0.matches("^\\d{6}$"));
        assertEquals(codeSlot0, codeSlot0End, "同一 30 秒窗口内核销码应保持稳定");
        assertNotEquals(codeSlot0, codeSlot1, "跨过 30 秒边界后核销码应自动刷新");

        // 新窗口第 5 秒（<8s 宽限期）仍兼容上一窗口的读码输入
        assertTrue(OfflineVerifyCodeUtils.verifyCode(101L, SEED, SECRET, codeSlot0, base + 35));
        // 超过 8 秒宽限期后，旧码必须彻底失效
        assertFalse(OfflineVerifyCodeUtils.verifyCode(101L, SEED, SECRET, codeSlot0, base + 39));
        // 当前窗口新码有效
        assertTrue(OfflineVerifyCodeUtils.verifyCode(101L, SEED, SECRET, codeSlot1, base + 39));
    }

    @Test
    void generateQrToken_rotatesEvery10SecondsAndRejectsExpiredScreenshot() {
        long base = 1795570200L; // 正好被 10 整除
        String qrSlot0 = OfflineVerifyCodeUtils.generateQrToken(202L, SEED, SECRET, base + 4);
        String qrSlot1 = OfflineVerifyCodeUtils.generateQrToken(202L, SEED, SECRET, base + 10);

        assertNotEquals(qrSlot0, qrSlot1, "跨过 10 秒边界后动态二维码应立即刷新");

        OfflineVerifyCodeUtils.ParsedQrToken parsed0 = OfflineVerifyCodeUtils.parseQrToken(qrSlot0);
        assertNotNull(parsed0);
        assertEquals(202L, parsed0.getOrderId());

        // 当前 10s 窗口内验证通过
        assertEquals(OfflineVerifyCodeUtils.QrVerifyStatus.VALID,
                OfflineVerifyCodeUtils.verifyQrToken(202L, SEED, SECRET, parsed0, base + 9));
        // 跨边界 2 秒内传输宽限有效
        assertEquals(OfflineVerifyCodeUtils.QrVerifyStatus.VALID,
                OfflineVerifyCodeUtils.verifyQrToken(202L, SEED, SECRET, parsed0, base + 12));
        // 超过 3 秒宽限后（模拟微信发送截图场景），必须返回 EXPIRED
        assertEquals(OfflineVerifyCodeUtils.QrVerifyStatus.EXPIRED,
                OfflineVerifyCodeUtils.verifyQrToken(202L, SEED, SECRET, parsed0, base + 14));

        // 篡改签名必须返回 INVALID
        String tampered = qrSlot0.substring(0, qrSlot0.length() - 1)
                + (qrSlot0.endsWith("a") ? "b" : "a");
        OfflineVerifyCodeUtils.ParsedQrToken parsedTampered = OfflineVerifyCodeUtils.parseQrToken(tampered);
        assertEquals(OfflineVerifyCodeUtils.QrVerifyStatus.INVALID,
                OfflineVerifyCodeUtils.verifyQrToken(202L, SEED, SECRET, parsedTampered, base + 5));

        // 订单不匹配必须返回 INVALID
        assertEquals(OfflineVerifyCodeUtils.QrVerifyStatus.INVALID,
                OfflineVerifyCodeUtils.verifyQrToken(999L, SEED, SECRET, parsed0, base + 5));

        // 非法字符串解析返回 null
        assertNull(OfflineVerifyCodeUtils.parseQrToken("https://example.com/fake"));
    }
}
