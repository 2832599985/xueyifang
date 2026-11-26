package com.xueyifang.common;

/**
 * 错误码枚举（v3.0 商业化扩展）
 *
 * @author xueyifang
 */
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAMS_ERROR(40001, "请求参数错误"),
    NOT_FOUND_ERROR(40002, "请求数据不存在"),
    NO_AUTH_ERROR(40003, "无权限访问"),
    FORBIDDEN_ERROR(40004, "禁止访问"),
    DUPLICATE_SUBMIT(40005, "请勿重复提交"),
    RATE_LIMIT_EXCEEDED(40006, "请求过于频繁"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),

    // 用户与实名信用相关错误码
    USER_NOT_EXIST(40101, "用户不存在"),
    USER_PASSWORD_ERROR(40102, "密码错误"),
    USER_NOT_LOGIN(40103, "用户未登录"),
    USER_ACCOUNT_DISABLED(40104, "账号已被禁用"),
    USER_STUDENT_ID_EXIST(40105, "学号已存在"),
    USER_NO_PUBLISH_PERMISSION(40106, "无发布权限"),
    USER_PERMISSION_REVIEWING(40107, "权限审核中"),
    USER_PERMISSION_REJECTED(40108, "权限申请已被拒绝"),
    USER_EDU_UNVERIFIED(40109, "请先完成校园教育邮箱实名核验"),
    USER_CREDIT_TOO_LOW(40110, "校园履约信用分过低，暂不可执行该操作"),

    // 服务与预约档期相关错误码
    SERVICE_NOT_EXIST(40201, "服务不存在"),
    SERVICE_OFFLINE(40202, "服务已下架"),
    SERVICE_REVIEWING(40203, "服务审核中"),
    SERVICE_REJECTED(40204, "服务审核未通过"),
    SERVICE_SOLD_OUT(40205, "服务已售罄"),
    SERVICE_NOT_OWNER(40206, "非服务发布者，无权操作"),
    SERVICE_CANNOT_EDIT(40207, "当前状态不允许编辑"),
    SERVICE_CANNOT_ONLINE(40208, "当前状态不允许上架"),
    SERVICE_CANNOT_OFFLINE(40209, "当前状态不允许下架"),
    SERVICE_SLOT_FULL(40210, "所选预约时段名额已满"),

    // 订单、电子围栏与近场核销错误码
    ORDER_NOT_EXIST(40301, "订单不存在"),
    ORDER_STATUS_ERROR(40302, "订单状态异常"),
    ORDER_PAYMENT_ERROR(40303, "订单支付失败"),
    ORDER_BALANCE_NOT_ENOUGH(40304, "余额不足"),
    GEOFENCE_OUT_OF_BOUNDS(40305, "所选位置超出本校区安全电子围栏范围"),
    MEETUP_PROPOSAL_INVALID(40306, "会面地点协商提案不存在或已失效"),
    BLE_VERIFY_FAILED(40307, "蓝牙近场加密碰面核验未通过"),

    // 权限相关错误码
    PERMISSION_DENIED(40401, "权限不足"),
    PERMISSION_ALREADY_APPLIED(40402, "已申请过权限"),

    // 纠纷相关错误码
    DISPUTE_NOT_EXIST(40501, "纠纷不存在"),
    DISPUTE_STATUS_ERROR(40502, "纠纷状态异常"),

    // Token相关错误码
    TOKEN_INVALID(40601, "Token无效"),
    TOKEN_EXPIRED(40602, "Token已过期"),

    // 钱包与商业支付相关错误码
    WALLET_BALANCE_NOT_ENOUGH(40801, "钱包余额不足"),
    WALLET_FROZEN_NOT_ENOUGH(40802, "冻结金额不足"),
    WALLET_RECHARGE_FAILED(40803, "充值失败"),
    WALLET_WITHDRAW_FAILED(40804, "提现失败"),
    WALLET_AMOUNT_INVALID(40805, "金额无效"),
    WALLET_DEMO_DISABLED(40806, "正式商业环境已关闭虚拟直充，请在订单收银台使用微信/支付宝担保支付"),

    // 聊天相关错误码
    CHAT_RECEIVER_NOT_EXIST(40701, "接收者不存在"),
    CHAT_MESSAGE_NOT_EXIST(40702, "消息不存在"),
    CHAT_CONTENT_EMPTY(40703, "消息内容不能为空"),
    CHAT_CANNOT_SEND_TO_SELF(40704, "不能给自己发送消息"),
    CHAT_RECEIVER_ACCOUNT_DISABLED(40705, "接收者账号已被禁用"),
    CHAT_BYPASS_BLOCKED(40706, "禁止发送站外收款链接，请通过平台担保订单交易");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
