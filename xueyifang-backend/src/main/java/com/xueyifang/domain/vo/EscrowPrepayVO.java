package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class EscrowPrepayVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String paymentNo;
    private Long orderId;
    private String payChannel;
    private BigDecimal amount;
    /**
     * 供 uni.requestPayment / wx.requestPayment 直接调起收银台的参数包
     */
    private Map<String, String> sdkPayParams;
    /**
     * 沙盒联调签名令牌（仅在非生产沙盒网关模式下返回，用于端到端验签回调联调）
     */
    private String sandboxCallbackSign;
}
