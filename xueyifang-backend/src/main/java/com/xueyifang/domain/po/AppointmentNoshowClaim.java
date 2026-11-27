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
 * 预约爽约自动赔付存证与划扣明细表实体（v4.4）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("appointment_noshow_claim")
@ApiModel(value = "AppointmentNoshowClaim对象", description = "预约爽约自动赔付存证与划扣明细表")
public class AppointmentNoshowClaim implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("赔付单号（NSH...）")
    private String claimNo;

    @ApiModelProperty("关联担保订单ID")
    private Long orderId;

    @ApiModelProperty("关联订单号快照")
    private String orderNumber;

    @ApiModelProperty("关联技能服务ID")
    private Long serviceId;

    @ApiModelProperty("买方用户ID")
    private Long buyerId;

    @ApiModelProperty("卖方用户ID")
    private Long sellerId;

    @ApiModelProperty("所属校区ID")
    private Long campusId;

    @ApiModelProperty("爽约责任类型（1=卖方逾期爽约未履约, 2=买方临期违约退单空驶补偿）")
    private Integer noshowType;

    @ApiModelProperty("原定预约开始时间")
    private LocalDateTime scheduledStart;

    @ApiModelProperty("原定预约结束时间")
    private LocalDateTime scheduledEnd;

    @ApiModelProperty("原订单托管总额")
    private BigDecimal orderAmount;

    @ApiModelProperty("退回买方本金金额")
    private BigDecimal refundToBuyerAmount;

    @ApiModelProperty("违约方划扣补偿给守约方金额")
    private BigDecimal compensationAmount;

    @ApiModelProperty("赔付资金来源（SELLER_DEPOSIT / BUYER_ESCROW）")
    private String compensationSource;

    @ApiModelProperty("违约方扣减履约信用分值")
    private Integer creditDeducted;

    @ApiModelProperty("状态（1=已自动完成资金划拨与赔付）")
    private Integer claimStatus;

    @ApiModelProperty("爽约判定依据与资金划转说明")
    private String evidenceNote;

    private LocalDateTime createTime;
}
