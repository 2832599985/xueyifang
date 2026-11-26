package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 纠纷详情VO
 *
 * @author xueyifang
 * @date 2025-11-21
 */
@Data
public class DisputeDetailVO implements Serializable {

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
     * 服务标题
     */
    private String serviceTitle;

    /**
     * 订单金额
     */
    private BigDecimal totalAmount;

    /**
     * 订单当前冻结担保金额（>0代表可原路退回）
     */
    private BigDecimal frozenAmount;

    /**
     * 关联订单状态
     */
    private Integer orderStatus;

    /**
     * 关联订单交易类型（1=线下, 2=线上）
     */
    private Integer tradeType;

    /**
     * 买家ID
     */
    private Long buyerId;

    /**
     * 买家姓名
     */
    private String buyerName;

    /**
     * 卖家ID
     */
    private Long sellerId;

    /**
     * 卖家姓名
     */
    private String sellerName;

    /**
     * 买家头像
     */
    private String buyerAvatar;

    /**
     * 卖家头像
     */
    private String sellerAvatar;

    /**
     * 纠纷发起人ID
     */
    private Long disputeInitiatorId;

    /**
     * 纠纷类型
     */
    private Integer disputeType;

    /**
     * 纠纷描述
     */
    private String description;

    /**
     * 纠纷举证图片列表
     */
    private List<String> evidenceImages;

    /**
     * 卖方发货/履约交付说明（供仲裁比对）
     */
    private String deliveryNote;

    /**
     * 卖方交付凭证图片列表（供仲裁比对）
     */
    private List<String> deliveryImages;

    /**
     * 纠纷状态
     */
    private Integer disputeStatus;

    /**
     * 管理员回复
     */
    private String adminReply;

    /**
     * 解决方案
     */
    private String resolution;

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

    public void setServiceTitle(String serviceTitle) {
        this.serviceTitle = serviceTitle;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }
}
