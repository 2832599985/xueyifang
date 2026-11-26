package com.xueyifang.controller;

import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.dto.ServiceSlotBatchRequest;
import com.xueyifang.domain.vo.ServiceTimeSlotVO;
import com.xueyifang.service.IServiceTimeSlotService;
import com.xueyifang.utils.UserContext;
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

@RestController
@RequestMapping("/service")
@Api(tags = "服务时间档期预约模块")
public class ServiceSlotController {

    @Resource
    private IServiceTimeSlotService serviceTimeSlotService;

    @GetMapping("/{serviceId}/slots")
    @ApiOperation("查询指定服务的未来可预约时间档期（公开）")
    public BaseResponse<List<ServiceTimeSlotVO>> getServiceSlots(@PathVariable Long serviceId) {
        return ResultUtils.success(serviceTimeSlotService.listAvailableSlots(serviceId));
    }

    @PostMapping("/{serviceId}/slots/batch")
    @AuthCheck(needPublishPermission = true)
    @ApiOperation("卖家批量设置服务可预约档期")
    public BaseResponse<Void> saveServiceSlots(
            @PathVariable Long serviceId,
            @RequestBody ServiceSlotBatchRequest request) {
        serviceTimeSlotService.replaceServiceSlots(serviceId, UserContext.getCurrentUserId(), request);
        return ResultUtils.success();
    }
}
