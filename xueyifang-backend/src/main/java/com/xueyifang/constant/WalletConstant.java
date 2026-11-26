package com.xueyifang.constant;

/**
 * 钱包常量
 *
 * @author xueyifang
 */
public interface WalletConstant {

    /**
     * 交易类型
     */
    int TRANSACTION_TYPE_RECHARGE = 1;   // 充值
    int TRANSACTION_TYPE_WITHDRAW = 2;   // 提现
    int TRANSACTION_TYPE_PAYMENT = 3;    // 支付
    int TRANSACTION_TYPE_REFUND = 4;     // 退款
    int TRANSACTION_TYPE_INCOME = 5;     // 收入
    int TRANSACTION_TYPE_FREEZE = 6;     // 冻结
    int TRANSACTION_TYPE_UNFREEZE = 7;   // 解冻

    /**
     * 交易类型名称
     */
    String[] TRANSACTION_TYPE_NAMES = {
        "",      // 占位，索引从1开始
        "充值",   // 1
        "提现",   // 2
        "支付",   // 3
        "退款",   // 4
        "收入",   // 5
        "冻结",   // 6
        "解冻"    // 7
    };

    /**
     * 获取交易类型名称
     *
     * @param type 交易类型
     * @return 交易类型名称
     */
    static String getTransactionTypeName(int type) {
        if (type >= 1 && type < TRANSACTION_TYPE_NAMES.length) {
            return TRANSACTION_TYPE_NAMES[type];
        }
        return "未知";
    }
}
