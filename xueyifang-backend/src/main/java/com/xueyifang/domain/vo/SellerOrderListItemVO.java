package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 卖家订单列表项VO
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "SellerOrderListItemVO", description = "卖家订单列表项")
public class SellerOrderListItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单ID")
    private Long id;

    @ApiModelProperty(value = "订单编号")
    private String orderNumber;

    @ApiModelProperty(value = "服务ID")
    private Long serviceId;

    @ApiModelProperty(value = "服务标题")
    private String serviceTitle;

    @ApiModelProperty(value = "服务图片")
    private String serviceImage;

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "数量")
    private Integer quantity;

    @ApiModelProperty(value = "总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "交易类型（1=线下, 2=线上）")
    private Integer tradeType;

    @ApiModelProperty(value = "订单状态")
    private Integer orderStatus;

    @ApiModelProperty(value = "支付状态")
    private Integer paymentStatus;

    @ApiModelProperty(value = "买家ID")
    private Long buyerId;

    @ApiModelProperty(value = "买家名称")
    private String buyerName;

    @ApiModelProperty(value = "买家头像")
    private String buyerAvatar;

    @ApiModelProperty(value = "退款状态")
    private Integer refundStatus;

    @ApiModelProperty(value = "买家退款申请说明")
    private String refundReason;

    @ApiModelProperty(value = "订单备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
