package com.xueyifang.controller;

import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.po.AppointmentNoshowClaim;
import com.xueyifang.domain.po.FinancialReconciliationDaily;
import com.xueyifang.domain.vo.FinancialOverviewVO;
import com.xueyifang.service.IFinancialReconciliationService;
import com.xueyifang.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/finance")
@Api(tags = "管理端-平台财务日终自动对账报表与预约爽约自动赔付")
public class AdminFinanceController {

    @Resource
    private IFinancialReconciliationService financialReconciliationService;

    @GetMapping("/overview")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("获取平台财务日终对账、托管资金池与爽约赔付全局态势")
    public BaseResponse<FinancialOverviewVO> getOverview() {
        return ResultUtils.success(financialReconciliationService.getFinancialOverview());
    }

    @PostMapping("/reconcile")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("生成或刷新指定账期的日终自动对账报表")
    public BaseResponse<FinancialReconciliationDaily> reconcileDaily(
            @RequestParam(value = "reportDate", required = false) String reportDate) {
        LocalDate date = StringUtils.hasText(reportDate) ? LocalDate.parse(reportDate.trim()) : LocalDate.now();
        return ResultUtils.success(financialReconciliationService.generateOrRefreshDailyReport(date));
    }

    @GetMapping("/export-csv")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("导出指定日期区间平台财务日终对账单与分账/爽约赔付明细 CSV（含 UTF-8 BOM，兼容 Excel）")
    public BaseResponse<Map<String, String>> exportCsv(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        LocalDate start = StringUtils.hasText(startDate) ? LocalDate.parse(startDate.trim()) : LocalDate.now().minusDays(30);
        LocalDate end = StringUtils.hasText(endDate) ? LocalDate.parse(endDate.trim()) : LocalDate.now();
        String csvContent = financialReconciliationService.exportFinancialReconciliationCsv(start, end);
        Map<String, String> payload = new HashMap<>();
        payload.put("fileName", "xueyifang_finance_reconciliation_" + start + "_to_" + end + ".csv");
        payload.put("csvContent", csvContent);
        return ResultUtils.success(payload);
    }

    @PostMapping("/noshow/scan")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("立即巡检所有逾期未履约预约订单并执行自动退一赔十爽约赔付")
    public BaseResponse<Integer> scanOverdueNoShowOrders() {
        return ResultUtils.success(financialReconciliationService.scanAndCompensateOverdueNoShowOrders());
    }

    @PostMapping("/noshow/{orderId}/compensate")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("针对指定逾期预约订单执行卖方爽约自动退本金+保证金划扣赔付")
    public BaseResponse<AppointmentNoshowClaim> compensateOrder(
            @PathVariable("orderId") Long orderId,
            @RequestBody(required = false) Map<String, String> body) {
        Long adminId = UserContext.getCurrentUserId();
        String note = body != null ? body.get("evidenceNote") : null;
        return ResultUtils.success(financialReconciliationService.processSellerNoShowCompensation(adminId, orderId, note));
    }
}
