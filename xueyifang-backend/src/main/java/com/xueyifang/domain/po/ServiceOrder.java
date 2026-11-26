package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务订单表（v3.0 商业版：含预约档期、精确地图会面坐标、双向锁定状态、平台抽佣分账与核销通道）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("service_order")
@ApiModel(value = "ServiceOrder对象", description = "服务订单表")
public class ServiceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNumber;
    private Long serviceId;
    private Long buyerId;
    private Long sellerId;

    @ApiModelProperty(value = "所属校区ID")
    private Long campusId;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer tradeType;
    private Long tradeLocationId;

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

    @ApiModelProperty(value = "约定会面POI建筑名")
    private String meetupPoiName;

    @ApiModelProperty(value = "约定会面详细说明")
    private String meetupAddress;
    private Integer meetupRadiusMeters;

    @ApiModelProperty(value = "双方是否已确认锁定会面地点（0=初始, 1=双方已确认锁定）")
    private Integer meetupConfirmed;

    private Integer paymentStatus;
    private Integer orderStatus;
    private BigDecimal frozenAmount;

    @ApiModelProperty(value = "平台技术服务费（抽佣）")
    private BigDecimal platformFee;

    @ApiModelProperty(value = "卖方实际结算入账金额")
    private BigDecimal sellerSettleAmount;

    private Integer paymentMethod;
    private LocalDateTime paymentTime;
    private LocalDateTime sellerShipTime;
    private LocalDateTime buyerConfirmTime;
    private Integer refundStatus;
    private String refundReason;
    private LocalDateTime refundRequestTime;
    private String remark;
    private String deliveryNote;
    private String deliveryImages;
    private String verifyCode;

    @ApiModelProperty(value = "核销通道（QR_10S / TOTP_30S / BLE_CRYPTO / ONLINE）")
    private String verifyChannel;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDelete;
}
