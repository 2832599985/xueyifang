package com.xueyifang.constant;

/**
 * 钱包与资金台账常量
 *
 * @author xueyifang
 */
public interface WalletConstant {

    int TRANSACTION_TYPE_RECHARGE = 1;      // 充值
    int TRANSACTION_TYPE_WITHDRAW = 2;      // 提现
    int TRANSACTION_TYPE_PAYMENT = 3;       // 担保支付
    int TRANSACTION_TYPE_REFUND = 4;        // 退款
    int TRANSACTION_TYPE_INCOME = 5;        // 卖方净收入
    int TRANSACTION_TYPE_FREEZE = 6;        // 担保冻结
    int TRANSACTION_TYPE_UNFREEZE = 7;      // 担保解冻
    int TRANSACTION_TYPE_PLATFORM_FEE = 8;  // 平台技术服务费扣除
    int TRANSACTION_TYPE_DEPOSIT = 9;       // 卖家保证金变动

    String[] TRANSACTION_TYPE_NAMES = {
        "",
        "充值",
        "提现",
        "支付",
        "退款",
        "收入",
        "冻结",
        "解冻",
        "平台服务费",
        "履约保证金"
    };

    static String getTransactionTypeName(int type) {
        if (type >= 1 && type < TRANSACTION_TYPE_NAMES.length) {
            return TRANSACTION_TYPE_NAMES[type];
        }
        return "未知";
    }
}
