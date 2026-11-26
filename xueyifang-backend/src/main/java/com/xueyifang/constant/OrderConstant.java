package com.xueyifang.constant;

/**
 * 订单常量
 *
 * @author xueyifang
 */
public interface OrderConstant {

    /**
     * 订单状态 - 新交易流程 (v2.0)
     * 流程: 待支付(1) -> 待发货(2) -> 待收货(3) -> 已完成(4)
     */
    int ORDER_UNPAID = 1;              // 待支付
    int ORDER_PENDING_SHIP = 2;        // 待发货（已支付，等待卖家发货）
    int ORDER_PENDING_RECEIPT = 3;     // 待收货（卖家已发货，等待买家确认）
    int ORDER_COMPLETED = 4;           // 已完成
    int ORDER_CANCELLED = 5;           // 已取消
    int ORDER_FAILED = 6;              // 交易失败（退款等）

    // 兼容旧代码的别名（已废弃，请使用新常量名）
    @Deprecated
    int ORDER_PAID_PENDING_CONFIRM = ORDER_PENDING_SHIP;
    @Deprecated
    int ORDER_PAID = ORDER_PENDING_SHIP;
    @Deprecated
    int ORDER_TRADING = ORDER_PENDING_RECEIPT;

    /**
     * 支付状态
     */
    int PAYMENT_UNPAID = 1;
    int PAYMENT_PAID = 2;
    int PAYMENT_REFUNDED = 3;

    /**
     * 支付方式
     */
    int PAYMENT_METHOD_WALLET = 1;

    /**
     * 交易类型
     */
    int TRADE_TYPE_OFFLINE = 1;   // 线下交易（面交）
    int TRADE_TYPE_ONLINE = 2;    // 线上担保交易

    /**
     * 退款状态
     */
    int REFUND_NONE = 0;          // 无退款
    int REFUND_PENDING = 1;       // 退款申请中（等待卖家处理）
    int REFUND_APPROVED = 2;      // 卖家同意退款（等待执行）
    int REFUND_REJECTED = 3;      // 卖家拒绝退款
    int REFUND_COMPLETED = 4;     // 已执行退款（资金已退回）
}

