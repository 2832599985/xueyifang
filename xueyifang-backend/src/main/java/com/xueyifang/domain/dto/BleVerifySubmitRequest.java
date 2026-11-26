package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 提交蓝牙近场双向挑战-应答加密核销请求
 */
@Data
public class BleVerifySubmitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String buyerNonce;
    private String sellerNonce;
    private Integer rssiDbm;
    private BigDecimal buyerLat;
    private BigDecimal buyerLng;
    private BigDecimal sellerLat;
    private BigDecimal sellerLng;
    private String buyerSignature;
    private String sellerSignature;
}
