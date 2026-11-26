package com.xueyifang.constant;

/**
 * 系统配置常量（v3.0 商业化扩展）
 *
 * @author xueyifang
 */
public interface SysConfigConstant {

    String REGISTER_ENABLED = "REGISTER_ENABLED";
    String REGISTER_ENABLED_DEFAULT = "1";
    String REVIEW_MODE = "REVIEW_MODE";
    String ORDER_UNPAID_TIMEOUT_HOURS = "ORDER_UNPAID_TIMEOUT_HOURS";
    String SELLER_REFUND_TIMEOUT_DAYS = "SELLER_REFUND_TIMEOUT_DAYS";
    String AUTO_CONFIRM_RECEIPT_DAYS = "AUTO_CONFIRM_RECEIPT_DAYS";
    String WALLET_SINGLE_RECHARGE_LIMIT = "WALLET_SINGLE_RECHARGE_LIMIT";
    String WALLET_DAILY_RECHARGE_LIMIT = "WALLET_DAILY_RECHARGE_LIMIT";

    /**
     * 平台担保交易技术服务费率（基点：500 = 5.00%）
     */
    String PLATFORM_COMMISSION_BPS = "PLATFORM_COMMISSION_BPS";

    /**
     * 是否开放演示虚拟直充（0=关闭走第三方担保收银台，1=本地测试开启）
     */
    String WALLET_DEMO_RECHARGE_ENABLED = "WALLET_DEMO_RECHARGE_ENABLED";

    /**
     * 发布服务最低校园信用分要求
     */
    String MIN_CREDIT_SCORE_FOR_PUBLISH = "MIN_CREDIT_SCORE_FOR_PUBLISH";

    /**
     * 下单购买最低校园信用分要求
     */
    String MIN_CREDIT_SCORE_FOR_ORDER = "MIN_CREDIT_SCORE_FOR_ORDER";

    /**
     * 蓝牙近场碰面核销最低 RSSI 阈值（例如 -78 dBm）
     */
    String BLE_MIN_RSSI_DBM = "BLE_MIN_RSSI_DBM";

    /**
     * 会面坐标偏离容差半径（米）
     */
    String MEETUP_GEOFENCE_TOLERANCE_METERS = "MEETUP_GEOFENCE_TOLERANCE_METERS";
}
