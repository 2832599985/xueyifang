-- ========================================
-- 钱包流水表创建脚本
-- 用于记录所有钱包相关交易（充值、提现、支付、退款等）
-- ========================================

CREATE TABLE IF NOT EXISTS wallet_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    transaction_type TINYINT NOT NULL COMMENT '交易类型（1=充值, 2=提现, 3=支付, 4=退款, 5=收入, 6=冻结, 7=解冻）',
    amount DECIMAL(10,2) NOT NULL COMMENT '交易金额（正数）',
    balance_before DECIMAL(10,2) NOT NULL COMMENT '交易前可用余额',
    balance_after DECIMAL(10,2) NOT NULL COMMENT '交易后可用余额',
    frozen_before DECIMAL(10,2) DEFAULT 0 COMMENT '交易前冻结金额',
    frozen_after DECIMAL(10,2) DEFAULT 0 COMMENT '交易后冻结金额',
    related_order_id BIGINT DEFAULT NULL COMMENT '关联订单ID（可选）',
    transaction_no VARCHAR(64) DEFAULT NULL COMMENT '流水号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_transaction_type (transaction_type) COMMENT '交易类型索引',
    INDEX idx_create_time (create_time) COMMENT '创建时间索引',
    FOREIGN KEY fk_wallet_user(user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY fk_wallet_order(related_order_id) REFERENCES service_order(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包流水表';

-- ========================================
-- 交易类型说明
-- ========================================
-- 1 = 充值（recharge）         - amount 增加到 balance
-- 2 = 提现（withdraw）         - amount 从 balance 扣减
-- 3 = 支付（payment）          - amount 从 balance 扣减（线下）或转到 frozen（线上）
-- 4 = 退款（refund）           - amount 从 frozen 返回到 balance
-- 5 = 收入（income）           - amount 增加到 balance（收到货款）
-- 6 = 冻结（freeze）           - amount 从 balance 转到 frozen
-- 7 = 解冻（unfreeze）         - amount 从 frozen 转到 balance（取消订单等）

-- ========================================
-- 查询验证
-- ========================================
SHOW CREATE TABLE wallet_transaction;
SELECT * FROM wallet_transaction LIMIT 10;
