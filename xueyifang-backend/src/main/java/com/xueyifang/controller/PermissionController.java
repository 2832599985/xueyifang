package com.xueyifang.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xueyifang.annotation.IdempotentSubmit;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.PermissionApplyRequest;
import com.xueyifang.domain.po.Notification;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.PermissionStatusVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/permission")
@Api(tags = "权限申请模块")
public class PermissionController {

    @Resource
    private IUserService userService;

    @Resource
    private INotificationService notificationService;

    @Resource
    private com.xueyifang.utils.RedisUtils redisUtils;

    @PostMapping("/apply")
    @ApiOperation(value = "申请发布权限")
    @IdempotentSubmit(lockTime = 3, message = "请勿重复申请")
    public BaseResponse<Map<String, Object>> apply(@RequestBody(required = false) PermissionApplyRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN, "请先登录");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        if (user.getPublishPermission() != null && user.getPublishPermission() == UserConstant.HAS_PUBLISH_PERMISSION) {
            Map<String, Object> data = new HashMap<>();
            data.put("status", "approved");
            data.put("message", "已拥有发布权限，无需申请");
            return ResultUtils.success(data);
        }
        if (user.getPermissionReviewStatus() != null && user.getPermissionReviewStatus() == UserConstant.PERMISSION_PENDING) {
            throw new BusinessException(ErrorCode.PERMISSION_ALREADY_APPLIED, "已提交申请，正在审核中");
        }
        User update = new User();
        update.setId(userId);
        update.setPermissionReviewStatus(UserConstant.PERMISSION_PENDING);
        if (request != null && request.getReason() != null && !request.getReason().trim().isEmpty()) {
            update.setPermissionApplyReason(request.getReason().trim());
        }
        boolean ok = userService.updateById(update);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "申请失败，请稍后重试");
        }
        if (redisUtils != null) {
            redisUtils.deleteUserCache(userId);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("status", "pending");
        return ResultUtils.success(data);
    }

    @GetMapping("/status")
    @ApiOperation(value = "查询权限审核状态")
    public BaseResponse<PermissionStatusVO> status() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN, "请先登录");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        PermissionStatusVO vo = new PermissionStatusVO();
        vo.setUserId(user.getId());
        vo.setStudentId(user.getStudentId());
        vo.setPublishPermission(user.getPublishPermission());
        vo.setPermissionReviewStatus(user.getPermissionReviewStatus());
        vo.setApplyReason(user.getPermissionApplyReason());
        boolean hasPermission = user.getPublishPermission() != null && user.getPublishPermission() == UserConstant.HAS_PUBLISH_PERMISSION;
        vo.setHasPermission(hasPermission);

        String status;
        String statusText;
        if (hasPermission || (user.getPermissionReviewStatus() != null && user.getPermissionReviewStatus() == UserConstant.PERMISSION_APPROVED)) {
            status = "approved";
            statusText = "已批准";
        } else if (user.getPermissionReviewStatus() != null && user.getPermissionReviewStatus() == UserConstant.PERMISSION_REJECTED) {
            status = "rejected";
            statusText = "已拒绝";
        } else if (user.getPermissionReviewStatus() != null && user.getPermissionReviewStatus() == UserConstant.PERMISSION_PENDING) {
            status = "pending";
            statusText = "审核中";
        } else {
            status = null;
            statusText = "未申请";
        }
        vo.setStatus(status);
        vo.setStatusText(statusText);

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getRecipientId, userId)
                .eq(Notification::getNotificationType, 1)
                .orderByDesc(Notification::getCreateTime)
                .last("limit 1");
        Notification latest = notificationService.getOne(wrapper, false);
        if (latest != null) {
            String lastResult;
            if ("approved".equals(status)) {
                lastResult = "approved";
            } else if ("rejected".equals(status)) {
                lastResult = "rejected";
            } else {
                lastResult = null;
            }
            vo.setLastReviewResult(lastResult);
            vo.setLastReviewReason(latest.getContent());
            LocalDateTime reviewedAt = latest.getCreateTime();
            vo.setReviewedAt(reviewedAt);
        }
        return ResultUtils.success(vo);
    }
}
