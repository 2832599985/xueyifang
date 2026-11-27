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
 * 平台财务日终自动对账报表实体（v4.4）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("financial_reconciliation_daily")
@ApiModel(value = "FinancialReconciliationDaily对象", description = "平台财务日终自动对账报表")
public class FinancialReconciliationDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("对账批次号（FIN...）")
    private String reportNo;

    @ApiModelProperty("对账账期（YYYY-MM-DD）")
    private String reportDate;

    @ApiModelProperty("当日支付担保订单笔数")
    private Integer paidOrderCount;

    @ApiModelProperty("当日完结结算订单笔数")
    private Integer completedOrderCount;

    @ApiModelProperty("当日退款订单笔数")
    private Integer refundOrderCount;

    @ApiModelProperty("当日预约爽约自动赔付笔数")
    private Integer noshowCompensatedCount;

    @ApiModelProperty("当日担保资金流入总额")
    private BigDecimal grossEscrowInflow;

    @ApiModelProperty("当日创作者结算入账净额")
    private BigDecimal sellerSettledAmount;

    @ApiModelProperty("当日平台技术服务费抽佣净额")
    private BigDecimal platformCommissionAmount;

    @ApiModelProperty("当日退回买方总额")
    private BigDecimal refundOutflowAmount;

    @ApiModelProperty("当日爽约违约金划扣赔付总额")
    private BigDecimal noshowPenaltyAmount;

    @ApiModelProperty("日终全平台在途托管冻结资金池余额")
    private BigDecimal activeEscrowFrozenPool;

    @ApiModelProperty("勾稽差异订单笔数")
    private Integer discrepancyCount;

    @ApiModelProperty("勾稽差额合计")
    private BigDecimal discrepancyAmount;

    @ApiModelProperty("对账结论（1=总分勾稽账平, 2=存在差额预警待复核）")
    private Integer reconcileStatus;

    @ApiModelProperty("自动对账审计摘要说明")
    private String auditNote;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
