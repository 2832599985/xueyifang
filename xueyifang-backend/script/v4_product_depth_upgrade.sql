-- ============================================================================
-- 学艺坊 (Xueyifang) v4.0 / v4.1 / v4.2 全纵深产品升级脚本
-- 包含：
--   1. C2B 反向悬赏与竞标引擎 (demand_request, demand_bid)
--   2. 大额订单分阶段里程碑托管结算与创作者保证金台账 (order_milestone, seller_deposit_ledger)
--   3. 校内多人拼班团课与共享辅导档期引擎 (group_session, group_session_participant)
-- 兼容 MySQL 8.0+，幂等可执行，Docker Compose 自动挂载为 03-v4-depth.sql
-- ============================================================================

SET NAMES utf8mb4;

-- 1. 校内反向悬赏需求表（买方发悬赏 -> 认证创作者竞标报价 -> 选标原子转担保订单）
CREATE TABLE IF NOT EXISTS `demand_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '悬赏需求ID',
  `demand_no` varchar(64) NOT NULL COMMENT '悬赏单号（DEMAND...）',
  `publisher_id` bigint NOT NULL COMMENT '发布需求的学生ID（买方）',
  `campus_id` bigint NOT NULL DEFAULT 1 COMMENT '所属校区ID',
  `tag_id` bigint DEFAULT NULL COMMENT '关联服务分类标签ID',
  `title` varchar(120) NOT NULL COMMENT '悬赏需求标题',
  `description` text NOT NULL COMMENT '详细需求背景、交付标准与时间安排',
  `budget_min` decimal(10,2) NOT NULL COMMENT '预算下限（元）',
  `budget_max` decimal(10,2) NOT NULL COMMENT '预算上限（元）',
  `trade_type` tinyint NOT NULL DEFAULT 1 COMMENT '交付方式（1=校园线下当面, 2=线上远程交付）',
  `urgency_level` tinyint NOT NULL DEFAULT 1 COMMENT '紧急程度（1=常规, 2=加急, 3=十万火急）',
  `deadline_time` datetime DEFAULT NULL COMMENT '期望最晚交付时间',
  `meetup_lat` decimal(10,6) DEFAULT NULL COMMENT '线下约定纬度（GCJ-02）',
  `meetup_lng` decimal(10,6) DEFAULT NULL COMMENT '线下约定经度（GCJ-02）',
  `meetup_poi_name` varchar(120) DEFAULT NULL COMMENT '线下约定校内地标',
  `meetup_address` varchar(255) DEFAULT NULL COMMENT '线下具体方位补充',
  `meetup_radius_meters` int DEFAULT 80 COMMENT '线下当面核销有效半径（米，10~2000）',
  `bid_count` int NOT NULL DEFAULT 0 COMMENT '当前收到的有效竞标数',
  `demand_status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0=招募竞标中, 1=已选标转担保单, 2=发布者取消, 3=管理员关闭）',
  `accepted_bid_id` bigint DEFAULT NULL COMMENT '采纳的中标记录ID',
  `matched_service_id` bigint DEFAULT NULL COMMENT '选标后自动生成的定向服务快照ID',
  `matched_order_id` bigint DEFAULT NULL COMMENT '选标后自动生成的担保订单ID',
  `close_reason` varchar(255) DEFAULT NULL COMMENT '取消或管理关闭原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删, 1=已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_demand_no` (`demand_no`),
  KEY `idx_demand_campus_status` (`campus_id`, `demand_status`, `urgency_level`),
  KEY `idx_demand_publisher` (`publisher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校内反向悬赏需求表';

-- 2. 创作者悬赏竞标报价表
CREATE TABLE IF NOT EXISTS `demand_bid` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '竞标ID',
  `demand_id` bigint NOT NULL COMMENT '悬赏需求ID',
  `bidder_id` bigint NOT NULL COMMENT '竞标创作者ID（卖方）',
  `bid_price` decimal(10,2) NOT NULL COMMENT '竞标报价金额（元）',
  `estimated_hours` int NOT NULL DEFAULT 24 COMMENT '承诺完成耗时（小时）',
  `bid_plan` varchar(600) NOT NULL COMMENT '接单方案、过往经验与可碰面时段说明',
  `bid_status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0=待选标, 1=已中标转单, 2=未中标, 3=已撤回）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '报价时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删, 1=已删）',
  PRIMARY KEY (`id`),
  KEY `idx_bid_demand` (`demand_id`, `bid_status`),
  KEY `idx_bid_bidder` (`bidder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作者悬赏竞标报价表';

-- 3. 大额技能订单分阶段里程碑托管结算表（v4.1）
CREATE TABLE IF NOT EXISTS `order_milestone` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '里程碑ID',
  `order_id` bigint NOT NULL COMMENT '关联担保订单ID',
  `stage_index` int NOT NULL COMMENT '阶段序号（1, 2, 3...）',
  `title` varchar(120) NOT NULL COMMENT '阶段里程碑名称（如：框架初稿验收 / 终稿源码交付）',
  `amount` decimal(10,2) NOT NULL COMMENT '本阶段对应托管金额（元）',
  `milestone_status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0=待卖方交付, 1=卖方已提交待验收, 2=买方已验收释放资金）',
  `proof_note` varchar(500) DEFAULT NULL COMMENT '卖方阶段交付成果说明',
  `submitted_time` datetime DEFAULT NULL COMMENT '卖方提交阶段成果时间',
  `released_time` datetime DEFAULT NULL COMMENT '买方验收释放款项时间',
  `platform_fee` decimal(10,2) DEFAULT 0.00 COMMENT '本阶段扣除平台技术服务费',
  `seller_net_amount` decimal(10,2) DEFAULT 0.00 COMMENT '本阶段卖方实际入账净额',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_milestone_order` (`order_id`, `stage_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='担保订单分阶段里程碑交付与部分释放表';

-- 4. 创作者履约保证金变动台账表（v4.1）
CREATE TABLE IF NOT EXISTS `seller_deposit_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ledger_no` varchar(64) NOT NULL COMMENT '保证金单号',
  `user_id` bigint NOT NULL COMMENT '创作者用户ID',
  `action_type` tinyint NOT NULL COMMENT '动作类型（1=缴纳冻结保证金, 2=无责退还至余额, 3=违约仲裁划扣赔付）',
  `amount` decimal(10,2) NOT NULL COMMENT '变动金额（元）',
  `deposit_before` decimal(10,2) NOT NULL COMMENT '变动前保证金余额',
  `deposit_after` decimal(10,2) NOT NULL COMMENT '变动后保证金余额',
  `remark` varchar(255) DEFAULT NULL COMMENT '说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deposit_ledger_no` (`ledger_no`),
  KEY `idx_deposit_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作者履约保证金变动台账表';

-- 5. 校内多人拼班团课与共享辅导档期主表（v4.2）
CREATE TABLE IF NOT EXISTS `group_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '拼班团课ID',
  `session_no` varchar(64) NOT NULL COMMENT '拼班单号（GRP...）',
  `creator_id` bigint NOT NULL COMMENT '开班创作者ID（卖方）',
  `campus_id` bigint NOT NULL DEFAULT 1 COMMENT '所属校区ID',
  `tag_id` bigint DEFAULT NULL COMMENT '关联服务分类标签ID',
  `title` varchar(120) NOT NULL COMMENT '拼班团课标题（如：高数下期末冲刺5人小班 / ACM动态规划专题夜课）',
  `description` text NOT NULL COMMENT '课程大纲、讲义资料与目标人群说明',
  `base_price` decimal(10,2) NOT NULL COMMENT '基础拼班单价（达到最低开班人数时的每人价格）',
  `tier_target_count` int NOT NULL DEFAULT 4 COMMENT '阶梯优惠触发人数（达到该人数全员自动享受阶梯优惠价并退还差价）',
  `tier_discount_price` decimal(10,2) NOT NULL COMMENT '阶梯满员优惠单价（元/人，小于等于 base_price）',
  `min_participants` int NOT NULL DEFAULT 2 COMMENT '最低成班人数',
  `max_participants` int NOT NULL DEFAULT 6 COMMENT '最高容纳人数上限',
  `joined_count` int NOT NULL DEFAULT 0 COMMENT '当前已参团人数',
  `tier_rebate_applied` tinyint NOT NULL DEFAULT 0 COMMENT '是否已触发阶梯退差价（0=未触发, 1=已全员退还差价）',
  `trade_type` tinyint NOT NULL DEFAULT 1 COMMENT '授课方式（1=校园线下当面小班, 2=线上共享会议）',
  `session_start` datetime NOT NULL COMMENT '开课开始时间',
  `session_end` datetime NOT NULL COMMENT '开课结束时间',
  `meetup_lat` decimal(10,6) DEFAULT NULL COMMENT '线下授课纬度（GCJ-02）',
  `meetup_lng` decimal(10,6) DEFAULT NULL COMMENT '线下授课经度（GCJ-02）',
  `meetup_poi_name` varchar(120) DEFAULT NULL COMMENT '线下授课校内地标（如：图书馆研讨室302）',
  `meetup_address` varchar(255) DEFAULT NULL COMMENT '线下具体门牌或方位',
  `meetup_radius_meters` int DEFAULT 80 COMMENT '线下当面签到有效核销半径（米，10~2000）',
  `session_status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0=招募拼班中, 1=已成团待开课, 2=已满员锁定, 3=已完结, 4=已取消）',
  `template_service_id` bigint DEFAULT NULL COMMENT '关联底层担保服务模板ID',
  `close_reason` varchar(255) DEFAULT NULL COMMENT '取消或关闭原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_session_no` (`session_no`),
  KEY `idx_group_campus_status` (`campus_id`, `session_status`, `session_start`),
  KEY `idx_group_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校内多人拼班团课与共享辅导档期表';

-- 6. 拼班团课参团学员与阶梯退差价明细表（v4.2）
CREATE TABLE IF NOT EXISTS `group_session_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参团记录ID',
  `session_id` bigint NOT NULL COMMENT '拼班团课ID',
  `buyer_id` bigint NOT NULL COMMENT '参团学生ID（买方）',
  `order_id` bigint NOT NULL COMMENT '关联独立担保订单ID（支持每位学员独立核销/仲裁）',
  `locked_price` decimal(10,2) NOT NULL COMMENT '参团时初始锁定单价',
  `effective_price` decimal(10,2) NOT NULL COMMENT '阶梯折算后实际执行单价',
  `rebate_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '成团阶梯自动退回差价金额',
  `join_status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1=已参团有效, 2=已退出/取消）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '参团时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_buyer` (`session_id`, `buyer_id`),
  KEY `idx_participant_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拼班团课参团学员与阶梯退差价明细表';

-- 7. 异常高频互刷与秒拍秒确风控审计预警表（v4.3）
CREATE TABLE IF NOT EXISTS `risk_audit_alert` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '风控预警ID',
  `alert_no` varchar(64) NOT NULL COMMENT '风控单号（RSK...）',
  `rule_code` varchar(64) NOT NULL COMMENT '命中规则编码（BIDIRECTIONAL_WASH_TRADE / HIGH_FREQ_SAME_PAIR / ULTRA_FAST_COMPLETION / LOW_PRICE_SCORE_BRUSHING）',
  `rule_name` varchar(120) NOT NULL COMMENT '命中风控规则名称',
  `severity` tinyint NOT NULL DEFAULT 2 COMMENT '风险等级（1=低风险提示, 2=中危可疑预警, 3=高危互刷阻断）',
  `risk_score` int NOT NULL DEFAULT 60 COMMENT '量化风险评分（0~100）',
  `order_id` bigint NOT NULL COMMENT '触发预警的关联订单ID',
  `order_number` varchar(64) DEFAULT NULL COMMENT '关联订单号快照',
  `service_id` bigint DEFAULT NULL COMMENT '关联技能服务ID',
  `buyer_id` bigint NOT NULL COMMENT '买方用户ID',
  `seller_id` bigint NOT NULL COMMENT '卖方用户ID',
  `campus_id` bigint DEFAULT 1 COMMENT '所属校区ID',
  `order_amount` decimal(10,2) DEFAULT 0.00 COMMENT '涉事订单金额（元）',
  `pair_order_count_7d` int DEFAULT 1 COMMENT '近7日同买卖对累计交易笔数',
  `reverse_order_count_7d` int DEFAULT 0 COMMENT '近7日买卖角色互换反向对倒笔数',
  `fulfill_duration_seconds` bigint DEFAULT NULL COMMENT '支付至确认收货耗时（秒）',
  `evidence_summary` varchar(600) NOT NULL COMMENT '风控取证摘要说明',
  `handle_status` tinyint NOT NULL DEFAULT 0 COMMENT '处置状态（0=待核查, 1=确认违规互刷并处罚, 2=排除误报放行）',
  `penalty_action` varchar(64) DEFAULT NULL COMMENT '执行处罚动作（DEDUCT_CREDIT_AND_OFFLINE / DEDUCT_CREDIT_ONLY / WARN_ONLY / DISMISSED）',
  `credit_deducted` int DEFAULT 0 COMMENT '双方各扣减履约信用分值',
  `handler_id` bigint DEFAULT NULL COMMENT '处置管理员ID',
  `handle_remark` varchar(500) DEFAULT NULL COMMENT '管理员核查结论与备注',
  `handle_time` datetime DEFAULT NULL COMMENT '处置时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '预警生成时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_risk_alert_no` (`alert_no`),
  UNIQUE KEY `uk_order_rule` (`order_id`, `rule_code`),
  KEY `idx_risk_status_severity` (`handle_status`, `severity`, `create_time`),
  KEY `idx_risk_pair` (`buyer_id`, `seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常高频互刷与反作弊风控审计预警表';

-- 8. 平台财务日终自动对账报表主表（v4.4）
CREATE TABLE IF NOT EXISTS `financial_reconciliation_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日终对账单ID',
  `report_no` varchar(64) NOT NULL COMMENT '对账批次号（FIN...）',
  `report_date` varchar(16) NOT NULL COMMENT '对账账期（YYYY-MM-DD）',
  `paid_order_count` int NOT NULL DEFAULT 0 COMMENT '当日支付担保订单笔数',
  `completed_order_count` int NOT NULL DEFAULT 0 COMMENT '当日完结结算订单笔数',
  `refund_order_count` int NOT NULL DEFAULT 0 COMMENT '当日退款订单笔数',
  `noshow_compensated_count` int NOT NULL DEFAULT 0 COMMENT '当日预约爽约自动赔付笔数',
  `gross_escrow_inflow` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '当日担保资金流入总额（元）',
  `seller_settled_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '当日创作者结算入账净额（元）',
  `platform_commission_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '当日平台技术服务费抽佣净额（元）',
  `refund_outflow_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '当日原路/钱包退回买方总额（元）',
  `noshow_penalty_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '当日爽约违约金划扣赔付总额（元）',
  `active_escrow_frozen_pool` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '日终全平台在途托管冻结资金池余额（元）',
  `discrepancy_count` int NOT NULL DEFAULT 0 COMMENT '分账或托管冻结勾稽差异订单笔数',
  `discrepancy_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '勾稽差额合计（元）',
  `reconcile_status` tinyint NOT NULL DEFAULT 1 COMMENT '对账结论（1=总分勾稽账平, 2=存在差额预警待复核）',
  `audit_note` varchar(500) DEFAULT NULL COMMENT '自动对账审计摘要说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reconcile_date` (`report_date`),
  UNIQUE KEY `uk_reconcile_no` (`report_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台财务日终自动对账报表';

-- 9. 预约爽约自动赔付存证与划扣明细表（v4.4）
CREATE TABLE IF NOT EXISTS `appointment_noshow_claim` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '爽约赔付单ID',
  `claim_no` varchar(64) NOT NULL COMMENT '赔付单号（NSH...）',
  `order_id` bigint NOT NULL COMMENT '关联担保订单ID',
  `order_number` varchar(64) DEFAULT NULL COMMENT '关联订单号快照',
  `service_id` bigint DEFAULT NULL COMMENT '关联技能服务ID',
  `buyer_id` bigint NOT NULL COMMENT '买方用户ID',
  `seller_id` bigint NOT NULL COMMENT '卖方用户ID',
  `campus_id` bigint DEFAULT 1 COMMENT '所属校区ID',
  `noshow_type` tinyint NOT NULL DEFAULT 1 COMMENT '爽约责任类型（1=卖方逾期爽约未履约, 2=买方临期违约退单空驶补偿）',
  `scheduled_start` datetime DEFAULT NULL COMMENT '原定预约开始时间',
  `scheduled_end` datetime DEFAULT NULL COMMENT '原定预约结束时间',
  `order_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '原订单托管总额（元）',
  `refund_to_buyer_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '退回买方本金金额（元）',
  `compensation_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '违约方划扣补偿给守约方金额（元）',
  `compensation_source` varchar(64) NOT NULL COMMENT '赔付资金来源（SELLER_DEPOSIT 卖方保证金划扣 / BUYER_ESCROW 买方托管金扣除）',
  `credit_deducted` int NOT NULL DEFAULT 10 COMMENT '违约方扣减履约信用分值',
  `claim_status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1=已自动完成资金划拨与赔付）',
  `evidence_note` varchar(500) NOT NULL COMMENT '爽约判定依据与资金划转说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '执行赔付时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_noshow_claim_no` (`claim_no`),
  UNIQUE KEY `uk_noshow_order` (`order_id`),
  KEY `idx_noshow_users` (`buyer_id`, `seller_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约爽约自动赔付存证与划扣明细表';

-- 10. 高并发热点查询复合覆盖索引优化（v4.5 幂等安全创建）
DROP PROCEDURE IF EXISTS `sp_xyf_add_index_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `sp_xyf_add_index_if_not_exists`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_columns VARCHAR(255)
)
BEGIN
    DECLARE v_index_count INT DEFAULT 0;
    SELECT COUNT(1) INTO v_index_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name;
    IF v_index_count = 0 THEN
        SET @sql_stmt = CONCAT('ALTER TABLE `', p_table_name, '` ADD INDEX `', p_index_name, '` (', p_index_columns, ')');
        PREPARE stmt FROM @sql_stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL `sp_xyf_add_index_if_not_exists`('service', 'idx_svc_campus_tag_status_price', '`campus_id`, `tag_id`, `status`, `is_delete`, `price`');
CALL `sp_xyf_add_index_if_not_exists`('service_order', 'idx_ord_buyer_status_time', '`buyer_id`, `order_status`, `is_delete`, `create_time`');
CALL `sp_xyf_add_index_if_not_exists`('service_order', 'idx_ord_seller_status_time', '`seller_id`, `order_status`, `is_delete`, `create_time`');
CALL `sp_xyf_add_index_if_not_exists`('service_order', 'idx_ord_sched_overdue', '`payment_status`, `order_status`, `scheduled_end`');
CALL `sp_xyf_add_index_if_not_exists`('wallet_transaction', 'idx_wtx_user_type_time', '`user_id`, `transaction_type`, `create_time`');
CALL `sp_xyf_add_index_if_not_exists`('notification', 'idx_notif_recip_read_time', '`recipient_id`, `is_read`, `create_time`');

DROP PROCEDURE IF EXISTS `sp_xyf_add_index_if_not_exists`;
