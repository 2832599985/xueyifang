package com.xueyifang.constant;

/**
 * 订单日志常量
 *
 * @author xueyifang
 * @date 2025-11-21
 */
public interface OrderLogConstant {

    // ==================== 操作类型 ====================

    /**
     * 创建订单
     */
    String ACTION_CREATE = "CREATE";

    /**
     * 支付订单
     */
    String ACTION_PAY = "PAY";

    /**
     * 卖家发货
     */
    String ACTION_SELLER_SHIP = "SELLER_SHIP";

    /**
     * 买家确认收货
     */
    String ACTION_BUYER_CONFIRM = "BUYER_CONFIRM";

    /**
     * 线下当面核销码核销完成
     */
    String ACTION_OFFLINE_VERIFY = "OFFLINE_VERIFY";

    /**
     * 卖家确认（已废弃，使用 ACTION_SELLER_SHIP）
     */
    @Deprecated
    String ACTION_SELLER_CONFIRM = "SELLER_CONFIRM";

    /**
     * 买家申请退款
     */
    String ACTION_REFUND_REQUEST = "REFUND_REQUEST";

    /**
     * 卖家同意退款
     */
    String ACTION_SELLER_APPROVE_REFUND = "SELLER_APPROVE_REFUND";

    /**
     * 卖家拒绝退款
     */
    String ACTION_SELLER_REJECT_REFUND = "SELLER_REJECT_REFUND";

    /**
     * 退款完成
     */
    String ACTION_REFUND = "REFUND";

    /**
     * 取消订单
     */
    String ACTION_CANCEL = "CANCEL";

    /**
     * 发起纠纷
     */
    String ACTION_DISPUTE_CREATE = "DISPUTE_CREATE";

    /**
     * 管理员退款（纠纷仲裁）
     */
    String ACTION_ADMIN_REFUND = "ADMIN_REFUND";

    /**
     * 系统自动取消（未支付超时）
     */
    String ACTION_AUTO_CANCEL = "AUTO_CANCEL";

    /**
     * 系统自动确认收货（发货后超时）
     */
    String ACTION_AUTO_CONFIRM_RECEIPT = "AUTO_CONFIRM_RECEIPT";

    /**
     * 系统自动退款（卖家处理超时）
     */
    String ACTION_AUTO_REFUND = "AUTO_REFUND";

    /**
     * 自动超时确认（已废弃）
     */
    @Deprecated
    String ACTION_AUTO_RELEASE = "AUTO_RELEASE";

    // ==================== 操作角色 ====================

    /**
     * 买家
     */
    Integer OPERATOR_ROLE_BUYER = 1;

    /**
     * 卖家
     */
    Integer OPERATOR_ROLE_SELLER = 2;

    /**
     * 管理员
     */
    Integer OPERATOR_ROLE_ADMIN = 3;

    /**
     * 系统
     */
    Integer OPERATOR_ROLE_SYSTEM = 4;
}
