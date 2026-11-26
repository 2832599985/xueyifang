package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.admin.AdminServiceReviewRequest;
import com.xueyifang.domain.vo.admin.PendingServiceVO;
import com.xueyifang.service.IAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 管理员服务审核控制器
 *
 * @author xueyifang
 */
@RestController
@RequestMapping("/admin/services")
@Api(tags = "管理员服务审核")
public class AdminServiceController {

    @Resource
    private IAdminService adminService;

    @GetMapping("/pending")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("获取待审核服务列表")
    public BaseResponse<Page<PendingServiceVO>> getPendingServices(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PendingServiceVO> result = adminService.getPendingServices(pageNum, pageSize);
        return ResultUtils.success(result);
    }

    @PutMapping("/service/review")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("审核服务发布申请")
    public BaseResponse<Void> reviewService(
            @RequestBody @Valid AdminServiceReviewRequest request) {
        adminService.reviewService(request.getServiceId(), request.getApproved(), request.getReason());
        return ResultUtils.success();
    }

    @GetMapping("/list")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("分页查询全量服务列表")
    public BaseResponse<Page<PendingServiceVO>> listAllServices(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return ResultUtils.success(adminService.listAllServices(pageNum, pageSize, status, keyword));
    }

    @PutMapping("/{serviceId}/offline")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("管理员强制下架违规服务")
    public BaseResponse<Void> forceOfflineService(
            @PathVariable("serviceId") Long serviceId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        adminService.forceOfflineService(serviceId, reason);
        return ResultUtils.success();
    }
}
