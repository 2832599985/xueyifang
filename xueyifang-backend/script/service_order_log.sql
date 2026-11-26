-- 订单状态/资金变更日志表
CREATE TABLE IF NOT EXISTS service_order_log (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_id BIGINT(20) NOT NULL COMMENT '订单ID',
    old_status TINYINT(4) DEFAULT NULL COMMENT '变更前订单状态',
    new_status TINYINT(4) DEFAULT NULL COMMENT '变更后订单状态',
    operator_id BIGINT(20) DEFAULT NULL COMMENT '操作人ID（为空表示系统自动）',
    operator_role TINYINT(4) DEFAULT NULL COMMENT '操作人角色（1=买家,2=卖家,3=管理员,4=系统）',
    action_type VARCHAR(50) NOT NULL COMMENT '动作类型（PAY/BUYER_CONFIRM/SELLER_CONFIRM/AUTO_RELEASE/REFUND/CANCEL/DISPUTE_CREATE等）',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT(4) DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_order_id(order_id),
    INDEX idx_action_type(action_type),
    INDEX idx_create_time(create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态/资金变更日志';

