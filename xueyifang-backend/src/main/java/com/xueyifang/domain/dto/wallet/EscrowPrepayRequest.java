package com.xueyifang.domain.dto.wallet;

import lombok.Data;

import java.io.Serializable;

/**
 * 商业化单笔担保支付预下单请求（微信小程序支付 / 微信App支付 / 支付宝）
 */
@Data
public class EscrowPrepayRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    /**
     * WECHAT_MINI / WECHAT_APP / ALIPAY_APP
     */
    private String payChannel;
}
