package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情VO
 *
 * @author xueyifang
 */
@Data
@ApiModel(value = "OrderDetailVO", description = "订单详情")
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单ID")
    private Long id;

    @ApiModelProperty(value = "订单编号")
    private String orderNumber;

    @ApiModelProperty(value = "服务ID")
    private Long serviceId;

    @ApiModelProperty(value = "服务标题")
    private String serviceTitle;

    @ApiModelProperty(value = "服务描述")
    private String serviceDescription;

    @ApiModelProperty(value = "服务图片")
    private String serviceImage;

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "数量")
    private Integer quantity;

    @ApiModelProperty(value = "总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "交易类型")
    private Integer tradeType;

    @ApiModelProperty(value = "交易地点ID")
    private Long tradeLocationId;

    @ApiModelProperty(value = "交易地点名称")
    private String tradeLocationName;

    @ApiModelProperty(value = "订单状态")
    private Integer orderStatus;

    @ApiModelProperty(value = "支付状态")
    private Integer paymentStatus;

    @ApiModelProperty(value = "支付方式")
    private Integer paymentMethod;

    @ApiModelProperty(value = "冻结金额")
    private BigDecimal frozenAmount;

    @ApiModelProperty(value = "买家ID")
    private Long buyerId;

    @ApiModelProperty(value = "买家名称")
    private String buyerName;

    @ApiModelProperty(value = "买家头像")
    private String buyerAvatar;

    @ApiModelProperty(value = "买家专业")
    private String buyerProfessional;

    @ApiModelProperty(value = "卖家ID")
    private Long sellerId;

    @ApiModelProperty(value = "卖家名称")
    private String sellerName;

    @ApiModelProperty(value = "卖家头像")
    private String sellerAvatar;

    @ApiModelProperty(value = "卖家专业")
    private String sellerProfessional;

    @ApiModelProperty(value = "支付时间")
    private LocalDateTime paymentTime;

    @ApiModelProperty(value = "卖家发货时间")
    private LocalDateTime sellerShipTime;

    @ApiModelProperty(value = "买家确认收货时间")
    private LocalDateTime buyerConfirmTime;

    @ApiModelProperty(value = "退款状态（0=无退款, 1=申请中, 2=卖家同意, 3=卖家拒绝）")
    private Integer refundStatus;

    @ApiModelProperty(value = "退款原因")
    private String refundReason;

    @ApiModelProperty(value = "退款申请时间")
    private LocalDateTime refundRequestTime;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "卖家发货/履约交付说明")
    private String deliveryNote;

    @ApiModelProperty(value = "卖家交付成果凭证图片列表")
    private List<String> deliveryImages;

    @ApiModelProperty(value = "线下交易6位当面核销码（仅买方本人可见）")
    private String verifyCode;

    @ApiModelProperty(value = "是否已评价")
    private Boolean isReviewed;

    public void setServiceTitle(String serviceTitle) {
        this.serviceTitle = serviceTitle;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public void setTradeLocationName(String tradeLocationName) {
        this.tradeLocationName = tradeLocationName;
    }
}
