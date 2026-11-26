-- 给 service 表添加 trade_location_id 字段（仅线下交易时使用）
ALTER TABLE service ADD COLUMN trade_location_id BIGINT(20) DEFAULT NULL COMMENT '交易地点ID（仅线下交易）' AFTER trade_type;
ALTER TABLE service ADD INDEX idx_trade_location_id(trade_location_id);
