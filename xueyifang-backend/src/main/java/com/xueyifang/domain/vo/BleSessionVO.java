package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 低功耗蓝牙（BLE）近场握手挑战会话凭证 VO
 */
@Data
public class BleSessionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String sessionId;
    private String advShortToken;
    private String serviceUuid;
    private String characteristicUuid;
    private String buyerNonce;
    private String roleSigningKey;
    private String role;
    private Integer minRssiDbm;
    private Integer expireSeconds;
}
