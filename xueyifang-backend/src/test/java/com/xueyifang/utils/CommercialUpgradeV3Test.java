package com.xueyifang.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.0 商业化核心引擎单元测试：校园多边形电子围栏、蓝牙双向挑战应答加密、站内信防跳单识别
 */
class CommercialUpgradeV3Test {

    @Test
    void geofencePolygonAndHaversineDistance_shouldAccuratelyClassifyCampusCoordinates() {
        String campusPolygon = "[[114.3465,30.5050],[114.3665,30.5050],[114.3665,30.5210],[114.3465,30.5210]]";

        // 校内图书馆坐标应在围栏内
        assertTrue(GeofenceUtils.isInsideCampusFence(30.5128, 114.3562, campusPolygon, 30.5128, 114.3562, 1800));

        // 校外 5 公里商业街坐标应被围栏拦截
        assertFalse(GeofenceUtils.isInsideCampusFence(30.5600, 114.4100, campusPolygon, 30.5128, 114.3562, 1800));

        // 两点距离约 111 米校验
        double dist = GeofenceUtils.distanceMeters(30.5120, 114.3560, 30.5130, 114.3560);
        assertTrue(dist > 105.0 && dist < 118.0);
    }

    @Test
    void bleChallengeResponseHandshake_shouldPassValidDualSignaturesAndRejectForgery() {
        String masterSecret = "unit-test-master-secret-2026";
        Long orderId = 8801L;
        String sessionId = BleMeetupCryptoUtils.generateSessionId();
        String buyerNonce = BleMeetupCryptoUtils.generateNonce();
        String sellerNonce = BleMeetupCryptoUtils.generateNonce();

        String buyerRoleKey = BleMeetupCryptoUtils.deriveRoleSigningKey(
                masterSecret, orderId, sessionId, BleMeetupCryptoUtils.ROLE_BUYER);
        String sellerRoleKey = BleMeetupCryptoUtils.deriveRoleSigningKey(
                masterSecret, orderId, sessionId, BleMeetupCryptoUtils.ROLE_SELLER);

        String buyerSig = BleMeetupCryptoUtils.computeHandshakeSignature(
                buyerRoleKey, orderId, sessionId, buyerNonce, sellerNonce, BleMeetupCryptoUtils.ROLE_BUYER);
        String sellerSig = BleMeetupCryptoUtils.computeHandshakeSignature(
                sellerRoleKey, orderId, sessionId, buyerNonce, sellerNonce, BleMeetupCryptoUtils.ROLE_SELLER);

        assertTrue(BleMeetupCryptoUtils.verifyHandshakeSignature(
                masterSecret, orderId, sessionId, buyerNonce, sellerNonce, BleMeetupCryptoUtils.ROLE_BUYER, buyerSig));
        assertTrue(BleMeetupCryptoUtils.verifyHandshakeSignature(
                masterSecret, orderId, sessionId, buyerNonce, sellerNonce, BleMeetupCryptoUtils.ROLE_SELLER, sellerSig));

        // 卖方试图用自己的子密钥伪造买方签名必须失败
        String forgedBuyerSig = BleMeetupCryptoUtils.computeHandshakeSignature(
                sellerRoleKey, orderId, sessionId, buyerNonce, sellerNonce, BleMeetupCryptoUtils.ROLE_BUYER);
        assertFalse(BleMeetupCryptoUtils.verifyHandshakeSignature(
                masterSecret, orderId, sessionId, buyerNonce, sellerNonce, BleMeetupCryptoUtils.ROLE_BUYER, forgedBuyerSig));

        String shortAdv = BleMeetupCryptoUtils.generateShortAdvToken();
        assertNotNull(shortAdv);
        assertEquals(13, shortAdv.length());
    }

    @Test
    void chatAntiBypass_shouldMaskPreEscrowContactsAndBlockPaymentUrls() {
        ChatAntiBypassUtils.SanitizeResult qrAttempt = ChatAntiBypassUtils.inspectAndSanitize(
                "直接扫我 https://qr.alipay.com/fkx12345 转我就行", false);
        assertTrue(qrAttempt.isBlocked());

        ChatAntiBypassUtils.SanitizeResult preEscrowWx = ChatAntiBypassUtils.inspectAndSanitize(
                "同学加我微信号 abc_campus88 或者打 13812345678 详聊", false);
        assertTrue(preEscrowWx.isTriggered());
        assertFalse(preEscrowWx.isBlocked());
        assertTrue(preEscrowWx.getSanitizedContent().contains("138****5678"));

        // 已支付担保单后允许正常沟通楼栋电话细节
        ChatAntiBypassUtils.SanitizeResult postEscrow = ChatAntiBypassUtils.inspectAndSanitize(
                "我到图书馆门口了，电话 13812345678", true);
        assertFalse(postEscrow.isTriggered());
    }
}
