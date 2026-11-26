package com.xueyifang.constant;

/**
 * 通知常量
 *
 * @author xueyifang
 */
public interface NotificationConstant {

    /**
     * 通知类型
     */
    int TYPE_PERMISSION_APPROVAL = 1;  // 权限批准
    int TYPE_SERVICE_REMOVAL = 2;      // 服务下架
    int TYPE_ORDER = 3;                // 订单通知
    int TYPE_DISPUTE = 4;              // 纠纷通知
    int TYPE_SERVICE_REVIEW = 5;       // 服务审核通知

    /**
     * 已读状态
     */
    int READ_STATUS_UNREAD = 0;        // 未读
    int READ_STATUS_READ = 1;          // 已读
}
