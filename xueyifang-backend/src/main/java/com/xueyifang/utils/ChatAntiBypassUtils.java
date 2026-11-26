package com.xueyifang.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 站内聊天防跳单（防私下导流转账）识别与脱敏引擎
 * <p>
 * 在订单未支付担保款前，对站内私信中的微信号导流话术、手机号、支付宝/收款外链进行智能识别与脱敏提示，
 * 既保护校园买卖双方免受校外诈骗，又守住平台担保交易闭环。
 */
public final class ChatAntiBypassUtils {

    private static final Pattern PHONE_IN_TEXT = Pattern.compile("(?<!\\d)(1[3-9]\\d)[\\s\\-]*(\\d{4})[\\s\\-]*(\\d{4})(?!\\d)");
    private static final Pattern WECHAT_HINT = Pattern.compile(
            "(?i)(加我?微|微信号|vx|wx号|v信|威信|私下转|走微信|走支付宝|扫我收款|脱离平台)\\s*[:：]?\\s*([a-zA-Z0-9_\\-]{5,20})?"
    );
    private static final Pattern PAYMENT_URL = Pattern.compile("(?i)(wxp://|https?://qr\\.alipay\\.com/|alipays://)");

    private ChatAntiBypassUtils() {
    }

    public static final class SanitizeResult {
        private final boolean triggered;
        private final boolean blocked;
        private final String sanitizedContent;
        private final String riskNotice;

        public SanitizeResult(boolean triggered, boolean blocked, String sanitizedContent, String riskNotice) {
            this.triggered = triggered;
            this.blocked = blocked;
            this.sanitizedContent = sanitizedContent;
            this.riskNotice = riskNotice;
        }

        public boolean isTriggered() {
            return triggered;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getSanitizedContent() {
            return sanitizedContent;
        }

        public String getRiskNotice() {
            return riskNotice;
        }
    }

    /**
     * 扫描并处理文本消息
     *
     * @param rawContent     原始聊天文本
     * @param hasPaidEscrow  买卖双方当前是否存在进行中的已支付担保订单
     */
    public static SanitizeResult inspectAndSanitize(String rawContent, boolean hasPaidEscrow) {
        if (StringUtils.isBlank(rawContent)) {
            return new SanitizeResult(false, false, rawContent, null);
        }
        // 任何阶段严禁直接发送外部收款码协议链接
        if (PAYMENT_URL.matcher(rawContent).find()) {
            return new SanitizeResult(true, true, null,
                    "检测到外部收款链接，脱离学艺坊担保私下转账无法享受校园仲裁保护，请通过平台订单支付");
        }

        // 若尚未建立已支付担保单，对微信号导流与手机号做自动脱敏并追加安全提示
        if (!hasPaidEscrow) {
            String masked = rawContent;
            boolean hit = false;
            if (PHONE_IN_TEXT.matcher(masked).find()) {
                masked = PHONE_IN_TEXT.matcher(masked).replaceAll("$1****$3");
                hit = true;
            }
            if (WECHAT_HINT.matcher(masked).find()) {
                masked = WECHAT_HINT.matcher(masked).replaceAll("[平台安全提示：下单前已折叠站外导流信息]");
                hit = true;
            }
            if (hit) {
                String finalContent = masked + "\n【系统安全提醒：未支付担保订单前请勿私下添加站外账号或直接转账，以免丧失平台资金保障】";
                return new SanitizeResult(true, false, finalContent,
                        "下单前站外联系方式已自动脱敏保护，支付担保订单后可在订单内使用双向地图选点与当面核销");
            }
        }
        return new SanitizeResult(false, false, rawContent, null);
    }
}
