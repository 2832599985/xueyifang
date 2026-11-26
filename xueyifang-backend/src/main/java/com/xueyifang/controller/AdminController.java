package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.annotation.AuthCheck;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.admin.AdminPermissionReviewRequest;
import com.xueyifang.domain.dto.admin.AdminUserStatusRequest;
import com.xueyifang.domain.vo.admin.AdminStatisticsVO;
import com.xueyifang.domain.vo.admin.AdminTrendVO;
import com.xueyifang.domain.vo.admin.AdminUserVO;
import com.xueyifang.domain.vo.admin.PendingUserVO;
import com.xueyifang.service.IAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 管理员控制器
 *
 * @author xueyifang
 */
@RestController
@RequestMapping("/admin")
@Api(tags = "管理员功能")
public class AdminController {

    @Resource
    private IAdminService adminService;

    @GetMapping("/users/pending")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("获取待审核用户列表")
    public BaseResponse<Page<PendingUserVO>> getPendingUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PendingUserVO> result = adminService.getPendingUsers(pageNum, pageSize);
        return ResultUtils.success(result);
    }

    @PutMapping("/permission/review")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("审核用户权限申请")
    public BaseResponse<Void> reviewPermission(
            @RequestBody @Valid AdminPermissionReviewRequest request) {
        adminService.reviewUserPermission(request.getUserId(), request.getApproved(), request.getReason());
        return ResultUtils.success();
    }

    @GetMapping("/statistics")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("获取系统统计数据")
    public BaseResponse<AdminStatisticsVO> getStatistics() {
        AdminStatisticsVO result = adminService.getSystemStatistics();
        return ResultUtils.success(result);
    }

    @GetMapping("/statistics/trend")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("获取近7天趋势统计数据")
    public BaseResponse<AdminTrendVO> getTrendStatistics() {
        AdminTrendVO result = adminService.getTrendStatistics();
        return ResultUtils.success(result);
    }

    @GetMapping("/users/list")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("分页查询全量用户列表")
    public BaseResponse<Page<AdminUserVO>> listUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer accountStatus,
            @RequestParam(required = false) Integer publishPermission) {
        return ResultUtils.success(adminService.listUsers(pageNum, pageSize, keyword, accountStatus, publishPermission));
    }

    @PutMapping("/users/status")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("更新用户状态或发布权限")
    public BaseResponse<Void> updateUserStatus(@RequestBody @Valid AdminUserStatusRequest request) {
        adminService.updateUserStatus(request);
        return ResultUtils.success();
    }

    @PutMapping("/users/{userId}/reset-password")
    @AuthCheck(role = UserConstant.ROLE_ADMIN)
    @ApiOperation("重置用户密码为默认密码 password123")
    public BaseResponse<Void> resetUserPassword(@PathVariable("userId") Long userId) {
        adminService.resetUserPassword(userId);
        return ResultUtils.success();
    }
}
