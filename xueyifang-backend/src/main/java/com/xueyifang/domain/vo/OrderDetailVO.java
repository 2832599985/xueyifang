package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情VO（v3.0 含地图会面坐标、双向锁定状态、预约时段、平台服务费与蓝牙近场核销标识）
 */
@Data
@ApiModel(value = "OrderDetailVO", description = "订单详情")
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNumber;
    private Long serviceId;
    private String serviceTitle;
    private String serviceDescription;
    private String serviceImage;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer tradeType;
    private Long campusId;
    private Long tradeLocationId;
    private String tradeLocationName;

    @ApiModelProperty(value = "预约档期ID")
    private Long slotId;

    @ApiModelProperty(value = "约定履约开始时间")
    private LocalDateTime scheduledStart;

    @ApiModelProperty(value = "约定履约结束时间")
    private LocalDateTime scheduledEnd;

    @ApiModelProperty(value = "约定会面纬度（GCJ-02）")
    private BigDecimal meetupLat;

    @ApiModelProperty(value = "约定会面经度（GCJ-02）")
    private BigDecimal meetupLng;

    @ApiModelProperty(value = "约定会面地图建筑名称")
    private String meetupPoiName;

    @ApiModelProperty(value = "约定会面详细补充说明")
    private String meetupAddress;
    private Integer meetupRadiusMeters;

    @ApiModelProperty(value = "双方是否已确认锁定会面点（0=初始, 1=已双方锁定）")
    private Integer meetupConfirmed;

    @ApiModelProperty(value = "是否支持/建议蓝牙近场加密碰面核销")
    private Integer requireBleVerify;

    private Integer orderStatus;
    private Integer paymentStatus;
    private Integer paymentMethod;
    private BigDecimal frozenAmount;
    private BigDecimal platformFee;
    private BigDecimal sellerSettleAmount;
    private String verifyChannel;

    private Long buyerId;
    private String buyerName;
    private String buyerAvatar;
    private String buyerProfessional;
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private String sellerProfessional;
    private LocalDateTime paymentTime;
    private LocalDateTime sellerShipTime;
    private LocalDateTime buyerConfirmTime;
    private Integer refundStatus;
    private String refundReason;
    private LocalDateTime refundRequestTime;
    private LocalDateTime createTime;
    private String remark;
    private String deliveryNote;
    private List<String> deliveryImages;
    private String verifyCode;
    private Boolean isReviewed;
}
