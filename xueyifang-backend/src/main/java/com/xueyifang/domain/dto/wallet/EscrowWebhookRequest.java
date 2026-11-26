package com.xueyifang.domain.dto.wallet;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class EscrowWebhookRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String paymentNo;
    private String channelTradeNo;
    private BigDecimal paidAmount;
    private Long timestamp;
    private String signature;
}
