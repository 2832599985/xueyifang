package com.xueyifang.constant;

/**
 * 系统配置常量
 *
 * @author xueyifang
 */
public interface SysConfigConstant {

    /**
     * 注册开关配置 key
     * value: 1=开启注册, 0=关闭注册
     */
    String REGISTER_ENABLED = "REGISTER_ENABLED";

    /**
     * 注册开关默认值（开启）
     */
    String REGISTER_ENABLED_DEFAULT = "1";

    /**
     * 审核模式配置 key
     */
    String REVIEW_MODE = "REVIEW_MODE";

    /**
     * 订单未支付超时小时数
     */
    String ORDER_UNPAID_TIMEOUT_HOURS = "ORDER_UNPAID_TIMEOUT_HOURS";

    /**
     * 卖家处理退款超时天数
     */
    String SELLER_REFUND_TIMEOUT_DAYS = "SELLER_REFUND_TIMEOUT_DAYS";

    /**
     * 自动确认收货天数
     */
    String AUTO_CONFIRM_RECEIPT_DAYS = "AUTO_CONFIRM_RECEIPT_DAYS";

    /**
     * 钱包单笔充值上限（元）
     */
    String WALLET_SINGLE_RECHARGE_LIMIT = "WALLET_SINGLE_RECHARGE_LIMIT";

    /**
     * 钱包单日累计充值上限（元）
     */
    String WALLET_DAILY_RECHARGE_LIMIT = "WALLET_DAILY_RECHARGE_LIMIT";
}
