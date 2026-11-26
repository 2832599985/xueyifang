package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.NotificationConstant;
import com.xueyifang.domain.po.Notification;
import com.xueyifang.domain.vo.NotificationVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.NotificationMapper;
import com.xueyifang.service.INotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.utils.UserContext;
import com.xueyifang.websocket.WebSocketMessage;
import com.xueyifang.websocket.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 通知表 服务实现类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-15
 */
@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    @Resource
    private WebSocketSessionManager webSocketSessionManager;

    @Resource
    private AsyncNotificationPusher asyncNotificationPusher;

    /**
     * 通过 WebSocket 异步推送通知给接收者
     */
    private void pushNotification(Notification notification) {
        // 事务提交后再异步推送，避免业务回滚后用户仍收到通知
        com.xueyifang.utils.TransactionUtils.afterCommit(
                () -> asyncNotificationPusher.pushNotificationAsync(notification));
    }

    @Override
    public void sendOrderNotification(Long recipientId, Long orderId, String title, String content) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setNotificationType(NotificationConstant.TYPE_ORDER);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(orderId);
        notification.setIsRead(NotificationConstant.READ_STATUS_UNREAD);
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());
        this.save(notification);
        // 通过 WebSocket 推送
        pushNotification(notification);
    }

    @Override
    public void sendDisputeNotification(Long recipientId, Long disputeId, String title, String content) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setNotificationType(NotificationConstant.TYPE_DISPUTE);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(disputeId);
        notification.setIsRead(NotificationConstant.READ_STATUS_UNREAD);
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());
        this.save(notification);
        // 通过 WebSocket 推送
        pushNotification(notification);
    }

    @Override
    public Page<NotificationVO> getMyNotifications(Integer pageNum, Integer pageSize, Integer notificationType) {
        Long userId = UserContext.getCurrentUserId();

        Page<Notification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getRecipientId, userId);
        if (notificationType != null) {
            queryWrapper.eq(Notification::getNotificationType, notificationType);
        }
        queryWrapper.orderByDesc(Notification::getCreateTime);

        Page<Notification> notificationPage = this.page(page, queryWrapper);

        // 转换VO
        List<NotificationVO> voList = notificationPage.getRecords().stream().map(notification -> {
            NotificationVO vo = new NotificationVO();
            BeanUtils.copyProperties(notification, vo);
            return vo;
        }).collect(Collectors.toList());

        Page<NotificationVO> resultPage = new Page<>(pageNum, pageSize, notificationPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public Long getUnreadCount() {
        Long userId = UserContext.getCurrentUserId();

        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getRecipientId, userId)
                .eq(Notification::getIsRead, NotificationConstant.READ_STATUS_UNREAD);

        return this.count(queryWrapper);
    }

    @Override
    public void markAsRead(Long notificationId) {
        Long userId = UserContext.getCurrentUserId();

        Notification notification = this.getById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "通知不存在");
        }

        // 验证权限：只能标记自己的通知
        if (!notification.getRecipientId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此通知");
        }

        notification.setIsRead(NotificationConstant.READ_STATUS_READ);
        notification.setUpdateTime(LocalDateTime.now());
        this.updateById(notification);
    }

    @Override
    public void markAllAsRead() {
        Long userId = UserContext.getCurrentUserId();

        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notification::getRecipientId, userId)
                .eq(Notification::getIsRead, NotificationConstant.READ_STATUS_UNREAD)
                .set(Notification::getIsRead, NotificationConstant.READ_STATUS_READ)
                .set(Notification::getUpdateTime, LocalDateTime.now());

        this.update(updateWrapper);
    }

    @Override
    public void sendPermissionNotification(Long recipientId, boolean isApproved, String reason) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setNotificationType(NotificationConstant.TYPE_PERMISSION_APPROVAL);
        notification.setTitle("权限审核结果");
        notification.setRelatedId(recipientId);  // 关联用户ID，支持前端跳转到用户信息

        if (isApproved) {
            notification.setContent("您的发布权限申请已通过，现在可以发布服务了！");
        } else {
            notification.setContent("您的发布权限申请未通过。原因：" + (reason != null ? reason : "未提供"));
        }

        notification.setIsRead(NotificationConstant.READ_STATUS_UNREAD);
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());
        this.save(notification);
        // 通过 WebSocket 推送
        pushNotification(notification);
    }

    @Override
    public void sendServiceReviewNotification(Long recipientId, Long serviceId, String serviceTitle, boolean isApproved, String reason) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setNotificationType(NotificationConstant.TYPE_SERVICE_REVIEW);
        notification.setTitle("服务审核结果");
        notification.setRelatedId(serviceId);  // 关联服务ID，支持前端跳转到服务详情

        if (isApproved) {
            notification.setContent("您的服务「" + serviceTitle + "」已通过审核并上架！");
        } else {
            notification.setContent("您的服务「" + serviceTitle + "」审核未通过。原因：" + (reason != null ? reason : "未提供"));
        }

        notification.setIsRead(NotificationConstant.READ_STATUS_UNREAD);
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());
        this.save(notification);
        // 通过 WebSocket 推送
        pushNotification(notification);
    }

    @Override
    public void sendServiceRemovalNotification(Long recipientId, Long serviceId, String serviceTitle, String reason) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setNotificationType(NotificationConstant.TYPE_SERVICE_REMOVAL);
        notification.setTitle("服务强制下架通知");
        notification.setRelatedId(serviceId);
        notification.setContent("您的服务「" + serviceTitle + "」已被管理员下架。原因：" + (reason != null && !reason.trim().isEmpty() ? reason.trim() : "违反平台服务规范"));
        notification.setIsRead(NotificationConstant.READ_STATUS_UNREAD);
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());
        this.save(notification);
        pushNotification(notification);
    }
}
