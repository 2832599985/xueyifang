package com.xueyifang.domain.vo;

import com.xueyifang.domain.po.AppointmentNoshowClaim;
import com.xueyifang.domain.po.FinancialReconciliationDaily;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Accessors(chain = true)
@ApiModel(value = "FinancialOverviewVO", description = "平台财务日终对账与爽约赔付全局态势视图")
public class FinancialOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("累计平台技术服务费抽佣总收入（元）")
    private BigDecimal totalPlatformCommission;

    @ApiModelProperty("累计创作者结算入账总净额（元）")
    private BigDecimal totalSellerSettled;

    @ApiModelProperty("当前在途订单托管冻结资金池总额（元）")
    private BigDecimal currentEscrowFrozenPool;

    @ApiModelProperty("当前全平台创作者履约保证金在池总额（元）")
    private BigDecimal currentSellerDepositPool;

    @ApiModelProperty("累计预约爽约自动赔付笔数")
    private Integer totalNoshowClaims;

    @ApiModelProperty("累计预约爽约违约金赔付总额（元）")
    private BigDecimal totalNoshowCompensationAmount;

    @ApiModelProperty("累计日终对账单份数")
    private Integer totalReconcileReports;

    @ApiModelProperty("存在勾稽差异待复核的对账单份数")
    private Integer discrepancyReportCount;

    @ApiModelProperty("近期日终对账单列表")
    private List<FinancialReconciliationDaily> recentReports;

    @ApiModelProperty("近期预约爽约自动赔付明细列表")
    private List<AppointmentNoshowClaim> recentNoshowClaims;
}
