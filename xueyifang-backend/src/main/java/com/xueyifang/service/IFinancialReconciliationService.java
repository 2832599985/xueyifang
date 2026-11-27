package com.xueyifang.service;

import com.xueyifang.domain.po.AppointmentNoshowClaim;
import com.xueyifang.domain.po.FinancialReconciliationDaily;
import com.xueyifang.domain.vo.FinancialOverviewVO;

import java.time.LocalDate;
import java.util.List;

public interface IFinancialReconciliationService {

    /**
     * 获取平台财务对账、托管资金池与爽约赔付全局态势
     */
    FinancialOverviewVO getFinancialOverview();

    /**
     * 生成或刷新指定账期的平台财务日终自动对账报表
     */
    FinancialReconciliationDaily generateOrRefreshDailyReport(LocalDate reportDate);

    /**
     * 导出指定日期区间的平台财务日终对账单、分账台账与爽约赔付 CSV（含 UTF-8 BOM，兼容 Excel 直开）
     */
    String exportFinancialReconciliationCsv(LocalDate startDate, LocalDate endDate);

    /**
     * 执行卖方逾期爽约自动全额退还托管本金 + 划扣卖方履约保证金赔付买方
     */
    AppointmentNoshowClaim processSellerNoShowCompensation(Long operatorId, Long orderId, String evidenceNote);

    /**
     * 自动巡检所有已逾期超过 30 分钟仍未履约的预约档期担保订单并触发爽约自动赔付
     */
    int scanAndCompensateOverdueNoShowOrders();

    /**
     * 查询某笔订单关联的爽约自动赔付记录（若存在）
     */
    AppointmentNoshowClaim getClaimByOrderId(Long orderId);

    /**
     * 查询近期爽约赔付记录列表
     */
    List<AppointmentNoshowClaim> listRecentNoshowClaims(int limit);
}
