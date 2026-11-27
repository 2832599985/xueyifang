package com.xueyifang.controller;

import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.OrderMilestonePlanRequest;
import com.xueyifang.domain.po.OrderMilestone;
import com.xueyifang.service.IOrderMilestoneService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
@Api(tags = "担保订单分阶段里程碑交付与部分释放接口")
public class OrderMilestoneController {

    @Resource
    private IOrderMilestoneService orderMilestoneService;

    @GetMapping("/{orderId}/milestones")
    @ApiOperation("查询订单分阶段里程碑列表")
    public BaseResponse<List<OrderMilestone>> listMilestones(@PathVariable("orderId") Long orderId) {
        return ResultUtils.success(orderMilestoneService.listByOrderId(orderId));
    }

    @PostMapping("/{orderId}/milestones/plan")
    @ApiOperation("配置或更新订单分阶段里程碑拆解计划")
    public BaseResponse<List<OrderMilestone>> savePlan(
            @PathVariable("orderId") Long orderId,
            @RequestBody OrderMilestonePlanRequest request) {
        return ResultUtils.success(orderMilestoneService.saveMilestonePlan(orderId, request));
    }

    @PostMapping("/milestone/{milestoneId}/submit")
    @ApiOperation("卖方提交阶段里程碑交付成果说明")
    public BaseResponse<Boolean> submitProof(
            @PathVariable("milestoneId") Long milestoneId,
            @RequestBody Map<String, String> body) {
        String proofNote = body != null ? body.get("proofNote") : null;
        orderMilestoneService.submitMilestoneProof(milestoneId, proofNote);
        return ResultUtils.success(true);
    }

    @PostMapping("/milestone/{milestoneId}/release")
    @ApiOperation("买方验收通过并按比例释放该阶段托管资金")
    public BaseResponse<Boolean> releaseStage(@PathVariable("milestoneId") Long milestoneId) {
        orderMilestoneService.releaseMilestone(milestoneId);
        return ResultUtils.success(true);
    }
}
