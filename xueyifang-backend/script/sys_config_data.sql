-- ========================================
-- 系统配置数据初始化脚本
-- 用于订单自动释放功能
-- ========================================

-- 自动释放超时小时数配置
INSERT INTO sys_config (config_key, config_value, description, is_enabled, create_time, update_time)
VALUES (
    'AUTO_RELEASE_FROZEN_HOURS',
    '72',
    '买家确认后，超过此小时数未卖家确认，系统将自动释放冻结资金',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description),
    update_time = NOW();

-- 冻结资金释放百分比配置
INSERT INTO sys_config (config_key, config_value, description, is_enabled, create_time, update_time)
VALUES (
    'FROZEN_RELEASE_PERCENTAGE',
    '100',
    '自动释放时，释放给卖家的冻结资金百分比（0-100）',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description),
    update_time = NOW();

-- 查询验证
SELECT * FROM sys_config WHERE config_key IN ('AUTO_RELEASE_FROZEN_HOURS', 'FROZEN_RELEASE_PERCENTAGE');

-- ========================================
-- 订单自动取消配置
-- 用于自动取消长期未支付订单功能
-- ========================================

-- 未支付订单自动取消超时小时数配置
INSERT INTO sys_config (config_key, config_value, description, is_enabled, create_time, update_time)
VALUES (
    'ORDER_UNPAID_TIMEOUT_HOURS',
    '24',
    '未支付订单超过此小时数后，系统将自动取消订单',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description),
    update_time = NOW();

-- 查询验证（包含新增配置）
SELECT * FROM sys_config WHERE config_key IN ('AUTO_RELEASE_FROZEN_HOURS', 'FROZEN_RELEASE_PERCENTAGE', 'ORDER_UNPAID_TIMEOUT_HOURS');

-- ========================================
-- 注册开关配置
-- 用于控制自助注册是否开启
-- ========================================

-- 注册开关配置（1=开启注册, 0=关闭注册）
INSERT INTO sys_config (config_key, config_value, description, is_enabled, create_time, update_time)
VALUES (
    'REGISTER_ENABLED',
    '1',
    '自助注册开关（1=开启注册, 0=关闭注册）',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description),
    update_time = NOW();

-- 查询验证（包含注册开关）
SELECT * FROM sys_config WHERE config_key = 'REGISTER_ENABLED';

