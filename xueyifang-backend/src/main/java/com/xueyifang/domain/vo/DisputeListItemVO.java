package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 纠纷列表项VO
 *
 * @author xueyifang
 * @date 2025-11-21
 */
@Data
public class DisputeListItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 纠纷ID
     */
    private Long disputeId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNumber;

    /**
     * 关联服务标题
     */
    private String serviceTitle;

    /**
     * 订单涉及金额
     */
    private BigDecimal totalAmount;

    /**
     * 纠纷类型
     */
    private Integer disputeType;

    /**
     * 纠纷状态
     */
    private Integer disputeStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    public void setDisputeId(Long disputeId) {
        this.disputeId = disputeId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setDisputeType(Integer disputeType) {
        this.disputeType = disputeType;
    }

    public void setDisputeStatus(Integer disputeStatus) {
        this.disputeStatus = disputeStatus;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
