package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.RiskAlertQueryRequest;
import com.xueyifang.domain.dto.RiskAlertResolveRequest;
import com.xueyifang.domain.vo.RiskAuditAlertVO;
import com.xueyifang.domain.vo.RiskRadarOverviewVO;
import com.xueyifang.service.IRiskRadarService;
import com.xueyifang.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/admin/risk-radar")
@Api(tags = "管理端-异常高频互刷与反作弊风控审计雷达")
public class AdminRiskRadarController {

    @Resource
    private IRiskRadarService riskRadarService;

    @GetMapping("/overview")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("获取风控审计雷达全局态势指标")
    public BaseResponse<RiskRadarOverviewVO> getOverview() {
        return ResultUtils.success(riskRadarService.getRadarOverview());
    }

    @GetMapping("/list")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("分页查询风控审计预警列表")
    public BaseResponse<Page<RiskAuditAlertVO>> listAlerts(RiskAlertQueryRequest request) {
        return ResultUtils.success(riskRadarService.pageAlerts(request));
    }

    @PostMapping("/scan")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("触发近N日订单交易图谱全量反互刷风控扫描")
    public BaseResponse<Integer> scanRecentOrders(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return ResultUtils.success(riskRadarService.scanRecentOrdersForRisk(days));
    }

    @PostMapping("/evaluate/{orderId}")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("针对单笔订单执行实时反作弊风控规则检测")
    public BaseResponse<List<RiskAuditAlertVO>> evaluateOrder(@PathVariable("orderId") Long orderId) {
        return ResultUtils.success(riskRadarService.evaluateOrderRisk(orderId));
    }

    @PostMapping("/{id}/resolve")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("管理员核查处置风控预警（执行双向信用分扣减、强制下架服务或排除误报）")
    public BaseResponse<RiskAuditAlertVO> resolveAlert(
            @PathVariable("id") Long id,
            @RequestBody RiskAlertResolveRequest request) {
        Long adminId = UserContext.getCurrentUserId();
        return ResultUtils.success(riskRadarService.resolveAlert(adminId, id, request));
    }
}
