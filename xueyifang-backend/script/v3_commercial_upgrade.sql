-- ============================================================================
-- 学艺坊 (Xueyifang) v3.0 商业化全栈升级数据库迁移脚本
-- 涵盖：多校区与多边形电子围栏、校园邮箱实名认证、履约信用分、卖家保证金、
--      服务档期预约、买卖双方双向地图选点协商、低功耗蓝牙(BLE)加密近场核销存证、
--      微信/支付宝单笔担保支付单据与平台技术服务费（抽佣）分账台账。
-- ============================================================================

USE `xueyifang`;

-- 1. 多校区与多边形电子围栏表
CREATE TABLE IF NOT EXISTS `campus` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '校区ID',
  `campus_code` varchar(32) NOT NULL COMMENT '校区编码',
  `campus_name` varchar(100) NOT NULL COMMENT '高校与校区名称',
  `city` varchar(64) DEFAULT '武汉市' COMMENT '所在城市',
  `center_lat` decimal(10,6) NOT NULL COMMENT '校区中心纬度（GCJ-02）',
  `center_lng` decimal(10,6) NOT NULL COMMENT '校区中心经度（GCJ-02）',
  `geofence_polygon` text COMMENT '电子围栏多边形顶点JSON数组 [[lng,lat],...]',
  `safe_radius_meters` int DEFAULT 1800 COMMENT '兜底安全半径（米）',
  `edu_email_domains` varchar(255) DEFAULT 'edu.cn' COMMENT '允许实名核验的校园邮箱域名（逗号分隔）',
  `status` tinyint DEFAULT 1 COMMENT '状态（1=启用, 0=停用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_campus_code` (`campus_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='高校多校区与电子围栏配置表';

INSERT INTO `campus` (`id`, `campus_code`, `campus_name`, `city`, `center_lat`, `center_lng`, `geofence_polygon`, `safe_radius_meters`, `edu_email_domains`, `status`)
VALUES
(1, 'MAIN_CAMPUS', '示范大学 · 主校区（南湖园区）', '武汉市', 30.512800, 114.356200,
 '[[114.3465,30.5050],[114.3665,30.5050],[114.3665,30.5210],[114.3465,30.5210]]', 1800, 'edu.cn', 1),
(2, 'EAST_CAMPUS', '示范大学 · 东院创新校区', '武汉市', 30.528600, 114.391500,
 '[[114.3830,30.5220],[114.4010,30.5220],[114.4010,30.5355],[114.3830,30.5355]]', 1500, 'edu.cn', 1)
ON DUPLICATE KEY UPDATE
  `campus_name` = VALUES(`campus_name`),
  `geofence_polygon` = VALUES(`geofence_polygon`),
  `update_time` = CURRENT_TIMESTAMP;

-- 2. 用户表扩展：多校区归属、校园教育邮箱真实验证、履约信用分与卖家保证金
ALTER TABLE `user`
  ADD COLUMN `campus_id` bigint DEFAULT 1 COMMENT '所属校区ID' AFTER `professional_id`,
  ADD COLUMN `edu_verified` tinyint DEFAULT 0 COMMENT '校园教育邮箱实名核验（0=未核验, 1=已核验）' AFTER `account_status`,
  ADD COLUMN `verified_edu_email` varchar(120) DEFAULT NULL COMMENT '已核验校园邮箱地址' AFTER `edu_verified`,
  ADD COLUMN `credit_score` int DEFAULT 100 COMMENT '履约信用分（初始100，上限120，低于60限制交易）' AFTER `verified_edu_email`,
  ADD COLUMN `seller_deposit` decimal(10,2) DEFAULT '0.00' COMMENT '卖家履约保证金余额' AFTER `credit_score`;

-- 将内置演示卖家升级为已通过校园邮箱核验状态
UPDATE `user` SET `edu_verified` = 1, `verified_edu_email` = CONCAT(`student_id`, '@stu.demo.edu.cn'), `credit_score` = 108 WHERE `publish_permission` = 1;

-- 3. 预设安全交接点表扩展经纬度与监控标识
ALTER TABLE `trade_location`
  ADD COLUMN `campus_id` bigint DEFAULT 1 COMMENT '所属校区ID' AFTER `id`,
  ADD COLUMN `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度（GCJ-02）' AFTER `location_address`,
  ADD COLUMN `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度（GCJ-02）' AFTER `latitude`,
  ADD COLUMN `has_cctv` tinyint DEFAULT 1 COMMENT '是否位于校园监控覆盖安全区（1=是, 0=否）' AFTER `longitude`;

UPDATE `trade_location` SET `campus_id` = 1, `latitude` = 30.513620, `longitude` = 114.355810, `has_cctv` = 1 WHERE `id` = 1 AND `latitude` IS NULL;
UPDATE `trade_location` SET `campus_id` = 1, `latitude` = 30.511450, `longitude` = 114.357920, `has_cctv` = 1 WHERE `id` = 2 AND `latitude` IS NULL;
UPDATE `trade_location` SET `campus_id` = 1, `latitude` = 30.514980, `longitude` = 114.353400, `has_cctv` = 1 WHERE `latitude` IS NULL;

-- 4. 服务表扩展：多校区、档期预约开关、退改规则与高价值蓝牙核销开关
ALTER TABLE `service`
  ADD COLUMN `campus_id` bigint DEFAULT 1 COMMENT '所属校区ID' AFTER `seller_id`,
  ADD COLUMN `require_slot` tinyint DEFAULT 0 COMMENT '是否启用时间段预约（0=随时履约, 1=需选档期）' AFTER `is_physical`,
  ADD COLUMN `cancel_free_hours` int DEFAULT 6 COMMENT '预约开始前免费退改小时数' AFTER `require_slot`,
  ADD COLUMN `require_ble_verify` tinyint DEFAULT 0 COMMENT '是否建议/启用蓝牙近场加密核销（0=普通扫码, 1=蓝牙+扫码高保障）' AFTER `cancel_free_hours`;

-- 5. 服务可预约档期表（解决约拍、课程辅导、清灰维修等 O2O 时间履约问题）
CREATE TABLE IF NOT EXISTS `service_time_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '档期ID',
  `service_id` bigint NOT NULL COMMENT '关联服务ID',
  `slot_start` datetime NOT NULL COMMENT '档期开始时间',
  `slot_end` datetime NOT NULL COMMENT '档期结束时间',
  `max_capacity` int NOT NULL DEFAULT 1 COMMENT '该时段可接待人数上限',
  `booked_count` int NOT NULL DEFAULT 0 COMMENT '已预约人数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1=可预约, 0=已关闭）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_slot_service_time` (`service_id`, `slot_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务预约时间档期表';

-- 6. 订单表扩展：精确地图会面坐标、预约时段、平台技术服务费与蓝牙/扫码核销通道留痕
ALTER TABLE `service_order`
  ADD COLUMN `campus_id` bigint DEFAULT 1 COMMENT '交易所属校区ID' AFTER `seller_id`,
  ADD COLUMN `slot_id` bigint DEFAULT NULL COMMENT '关联预约档期ID' AFTER `trade_location_id`,
  ADD COLUMN `scheduled_start` datetime DEFAULT NULL COMMENT '约定履约开始时间' AFTER `slot_id`,
  ADD COLUMN `scheduled_end` datetime DEFAULT NULL COMMENT '约定履约结束时间' AFTER `scheduled_start`,
  ADD COLUMN `meetup_lat` decimal(10,6) DEFAULT NULL COMMENT '约定会面纬度（GCJ-02）' AFTER `scheduled_end`,
  ADD COLUMN `meetup_lng` decimal(10,6) DEFAULT NULL COMMENT '约定会面经度（GCJ-02）' AFTER `meetup_lat`,
  ADD COLUMN `meetup_poi_name` varchar(120) DEFAULT NULL COMMENT '约定会面地图建筑/POI名称' AFTER `meetup_lng`,
  ADD COLUMN `meetup_address` varchar(255) DEFAULT NULL COMMENT '约定会面详细说明（如楼栋门厅/监控点）' AFTER `meetup_poi_name`,
  ADD COLUMN `meetup_confirmed` tinyint DEFAULT 0 COMMENT '双方是否已协商锁定会面点（0=初始选点, 1=双方已确认锁定）' AFTER `meetup_address`,
  ADD COLUMN `platform_fee` decimal(10,2) DEFAULT '0.00' COMMENT '平台技术服务费（抽佣金额）' AFTER `frozen_amount`,
  ADD COLUMN `seller_settle_amount` decimal(10,2) DEFAULT '0.00' COMMENT '卖方扣除服务费后实际结算金额' AFTER `platform_fee`,
  ADD COLUMN `verify_channel` varchar(32) DEFAULT NULL COMMENT '完结核销通道（QR_10S / TOTP_30S / BLE_CRYPTO / ONLINE）' AFTER `verify_code`;

-- 7. 买卖双方双向地图选点与时间协商提案表
CREATE TABLE IF NOT EXISTS `order_meetup_proposal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '提案ID',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `proposer_id` bigint NOT NULL COMMENT '发起方用户ID',
  `proposer_role` tinyint NOT NULL COMMENT '发起角色（1=买方, 2=卖方）',
  `latitude` decimal(10,6) NOT NULL COMMENT '提议会面纬度（GCJ-02）',
  `longitude` decimal(10,6) NOT NULL COMMENT '提议会面经度（GCJ-02）',
  `poi_name` varchar(120) NOT NULL COMMENT '地图选点名称（如：南湖图书馆正门）',
  `detail_address` varchar(255) DEFAULT NULL COMMENT '具体楼层或地标补充说明',
  `proposed_time` datetime DEFAULT NULL COMMENT '提议会面时间（可选）',
  `remark` varchar(255) DEFAULT NULL COMMENT '协商留言',
  `proposal_status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0=待对方确认, 1=对方已同意锁定, 2=对方已拒绝, 3=已被新提案覆盖）',
  `reject_reason` varchar(255) DEFAULT NULL COMMENT '拒绝或改约说明',
  `respond_time` datetime DEFAULT NULL COMMENT '对方响应时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_proposal_order` (`order_id`, `proposal_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单线下会面地点与时间双向协商提案表';

-- 8. 低功耗蓝牙（BLE）近场双向挑战签名核销存证表
CREATE TABLE IF NOT EXISTS `ble_verify_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `session_id` varchar(48) NOT NULL COMMENT '服务端签发的一次性挑战会话ID',
  `buyer_id` bigint NOT NULL COMMENT '买方用户ID',
  `seller_id` bigint NOT NULL COMMENT '卖方用户ID',
  `buyer_nonce` varchar(32) NOT NULL COMMENT '买方广播挑战随机数 Nb',
  `seller_nonce` varchar(32) NOT NULL COMMENT '卖方近场写入随机数 Ns',
  `rssi_dbm` int NOT NULL COMMENT '近场蓝牙信号强度 RSSI (dBm)',
  `buyer_lat` decimal(10,6) DEFAULT NULL COMMENT '买方核销时纬度',
  `buyer_lng` decimal(10,6) DEFAULT NULL COMMENT '买方核销时经度',
  `seller_lat` decimal(10,6) DEFAULT NULL COMMENT '卖方核销时纬度',
  `seller_lng` decimal(10,6) DEFAULT NULL COMMENT '卖方核销时经度',
  `distance_to_meetup_m` decimal(10,2) DEFAULT NULL COMMENT '距约定会面坐标物理误差（米）',
  `buyer_sig` varchar(64) NOT NULL COMMENT '买方设备 HMAC-SHA256 签名摘要',
  `seller_sig` varchar(64) NOT NULL COMMENT '卖方设备 HMAC-SHA256 签名摘要',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '核销存证时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ble_session` (`session_id`),
  KEY `idx_ble_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='蓝牙近场双向加密握手核销存证表';

-- 9. 商业化单笔担保支付网关单据表（对接微信小程序支付 / 微信App支付 / 支付宝）
CREATE TABLE IF NOT EXISTS `escrow_payment_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付单主键',
  `payment_no` varchar(64) NOT NULL COMMENT '平台支付单号',
  `order_id` bigint NOT NULL COMMENT '业务订单ID',
  `user_id` bigint NOT NULL COMMENT '付款买方ID',
  `pay_channel` varchar(32) NOT NULL COMMENT '支付渠道（WECHAT_MINI / WECHAT_APP / ALIPAY_APP / WALLET）',
  `amount` decimal(10,2) NOT NULL COMMENT '实付托管金额',
  `channel_trade_no` varchar(64) DEFAULT NULL COMMENT '微信/支付宝第三方交易流水号',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0=待支付, 1=已支付托管, 2=已关单, 3=已原路退款）',
  `paid_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `callback_digest` varchar(128) DEFAULT NULL COMMENT '异步回调验签摘要',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_payment_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业化第三方担保支付网关单据表';

-- 10. 平台技术服务费（抽佣）分账台账表
CREATE TABLE IF NOT EXISTS `platform_revenue_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ledger_no` varchar(64) NOT NULL COMMENT '分账流水号',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `seller_id` bigint NOT NULL COMMENT '卖方用户ID',
  `order_total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `commission_bps` int NOT NULL COMMENT '抽佣费率（基点，如500代表5.00%）',
  `commission_amount` decimal(10,2) NOT NULL COMMENT '平台抽佣收入金额',
  `seller_net_amount` decimal(10,2) NOT NULL COMMENT '卖方实际入账净额',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '分账结算时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_revenue_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台抽佣收入与担保分账台账表';

-- 11. 校园履约信用分变动流水表
CREATE TABLE IF NOT EXISTS `credit_score_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `delta` int NOT NULL COMMENT '分值变动（正加负减）',
  `score_before` int NOT NULL COMMENT '变动前信用分',
  `score_after` int NOT NULL COMMENT '变动后信用分',
  `reason_code` varchar(32) NOT NULL COMMENT '事件编码（EDU_VERIFY / ORDER_COMPLETE / DISPUTE_LOST / LATE_CANCEL）',
  `reason_desc` varchar(255) NOT NULL COMMENT '变动说明',
  `related_order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_credit_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户校园履约信用分变动流水表';

-- 12. 商业化风控与费率配置项注入
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`, `is_enabled`) VALUES
  ('PLATFORM_COMMISSION_BPS', '500', '平台担保交易技术服务费率（基点：500=5.00%，0=免佣）', 1),
  ('WALLET_DEMO_RECHARGE_ENABLED', '0', '是否开放演示环境虚拟余额直充（生产商业环境必须设为0，走微信/支付宝单笔担保支付）', 1),
  ('MIN_CREDIT_SCORE_FOR_PUBLISH', '70', '发布技能服务所需的最低校园信用分阈值', 1),
  ('MIN_CREDIT_SCORE_FOR_ORDER', '60', '发起担保购买所需的最低校园信用分阈值', 1),
  ('BLE_MIN_RSSI_DBM', '-78', '蓝牙近场碰面核销允许的最低信号强度（dBm，越大代表物理距离越近）', 1),
  ('MEETUP_GEOFENCE_TOLERANCE_METERS', '150', '线下核销允许偏离约定会面坐标的免警报容差半径（米）', 1)
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `update_time` = CURRENT_TIMESTAMP;
