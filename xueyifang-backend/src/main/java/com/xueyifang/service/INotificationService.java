package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.domain.po.Notification;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.vo.NotificationVO;

/**
 * <p>
 * 通知表 服务类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-15
 */
public interface INotificationService extends IService<Notification> {

    /**
     * 发送订单通知
     *
     * @param recipientId 接收者ID
     * @param orderId 订单ID
     * @param title 通知标题
     * @param content 通知内容
     */
    void sendOrderNotification(Long recipientId, Long orderId, String title, String content);

    /**
     * 发送纠纷通知
     *
     * @param recipientId 接收者ID
     * @param disputeId 纠纷ID
     * @param title 通知标题
     * @param content 通知内容
     */
    void sendDisputeNotification(Long recipientId, Long disputeId, String title, String content);

    /**
     * 获取我的通知列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param notificationType 通知类型（可选）
     * @return 通知列表
     */
    Page<NotificationVO> getMyNotifications(Integer pageNum, Integer pageSize, Integer notificationType);

    /**
     * 获取未读通知数量
     *
     * @return 未读数量
     */
    Long getUnreadCount();

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     */
    void markAsRead(Long notificationId);

    /**
     * 标记全部通知为已读
     */
    void markAllAsRead();

    /**
     * 发送权限审核通知
     *
     * @param recipientId 接收者ID（申请权限的用户）
     * @param isApproved true=批准, false=拒绝
     * @param reason 拒绝原因（拒绝时必填）
     */
    void sendPermissionNotification(Long recipientId, boolean isApproved, String reason);

    /**
     * 发送服务审核通知
     *
     * @param recipientId 接收者ID（服务所有者）
     * @param serviceId 服务ID（用于relatedId，支持前端跳转）
     * @param serviceTitle 服务标题
     * @param isApproved true=批准, false=拒绝
     * @param reason 拒绝原因（拒绝时必填）
     */
    void sendServiceReviewNotification(Long recipientId, Long serviceId, String serviceTitle, boolean isApproved, String reason);

    /**
     * 发送服务违规强制下架通知
     *
     * @param recipientId 接收者ID（卖家）
     * @param serviceId 服务ID
     * @param serviceTitle 服务标题
     * @param reason 下架原因
     */
    void sendServiceRemovalNotification(Long recipientId, Long serviceId, String serviceTitle, String reason);
}
