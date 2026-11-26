package com.xueyifang.domain.po;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 服务订单表
 * </p>
 *
 * @author luoyi
 * @since 2025-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("service_order")
@ApiModel(value="ServiceOrder对象", description="服务订单表")
public class ServiceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "订单编号")
    private String orderNumber;

    @ApiModelProperty(value = "服务ID")
    private Long serviceId;

    @ApiModelProperty(value = "购买者ID")
    private Long buyerId;

    @ApiModelProperty(value = "卖家ID")
    private Long sellerId;

    @ApiModelProperty(value = "购买数量")
    private Integer quantity;

    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "订单总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "交易类型（1=线下交易, 2=线上担保交易）")
    private Integer tradeType;

    @ApiModelProperty(value = "交易地点ID（仅线下交易）")
    private Long tradeLocationId;

    @ApiModelProperty(value = "支付状态（1=未支付, 2=已支付, 3=已退款）")
    private Integer paymentStatus;

    @ApiModelProperty(value = "订单状态（1=待支付, 2=待发货, 3=待收货, 4=已完成, 5=已取消, 6=交易失败）")
    private Integer orderStatus;

    @ApiModelProperty(value = "冻结金额（担保交易）")
    private BigDecimal frozenAmount;

    @ApiModelProperty(value = "支付方式（1=钱包余额）")
    private Integer paymentMethod;

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

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "卖家发货/履约交付说明")
    private String deliveryNote;

    @ApiModelProperty(value = "卖家交付成果凭证图片（逗号分隔相对路径）")
    private String deliveryImages;

    @ApiModelProperty(value = "线下交易6位当面核销码")
    private String verifyCode;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除")
    private Integer isDelete;


}
