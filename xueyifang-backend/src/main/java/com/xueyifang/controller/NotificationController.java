package com.xueyifang.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.domain.vo.NotificationVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.INotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 通知表 前端控制器
 * </p>
 *
 * @author luoyi
 * @since 2025-11-15
 */
@RestController
@RequestMapping("/notification")
@Api(tags = "通知管理")
public class NotificationController {

    @Resource
    private INotificationService notificationService;

    @GetMapping("/my-notifications")
    @ApiOperation("获取我的通知列表")
    public BaseResponse<Page<NotificationVO>> getMyNotifications(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer notificationType) {
        Page<NotificationVO> result = notificationService.getMyNotifications(pageNum, pageSize, notificationType);
        return ResultUtils.success(result);
    }

    @GetMapping("/unreadCount")
    @ApiOperation("获取未读通知数量")
    public BaseResponse<Long> getUnreadCount() {
        Long count = notificationService.getUnreadCount();
        return ResultUtils.success(count);
    }

    @PostMapping("/{id}/read")
    @ApiOperation("标记通知为已读")
    public BaseResponse<Void> markAsRead(@PathVariable Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        notificationService.markAsRead(id);
        return ResultUtils.success();
    }

    @PostMapping("/readAll")
    @ApiOperation("标记全部通知为已读")
    public BaseResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResultUtils.success();
    }
}
