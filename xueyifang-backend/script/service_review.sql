-- =============================================
-- 服务评价表
-- 创建时间：2025-11-30
-- 说明：存储买家对已完成订单的评价信息
-- =============================================

-- 创建服务评价表
CREATE TABLE IF NOT EXISTS service_review (
    id              BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    service_id      BIGINT(20)    NOT NULL COMMENT '服务ID',
    order_id        BIGINT(20)    NOT NULL COMMENT '订单ID',
    buyer_id        BIGINT(20)    NOT NULL COMMENT '评价人ID（买家）',
    seller_id       BIGINT(20)    NOT NULL COMMENT '卖家ID（冗余，方便统计）',
    rating          TINYINT(4)    NOT NULL COMMENT '评分（1-5）',
    content         VARCHAR(500)  NOT NULL COMMENT '评价内容（必填，10-500字）',
    is_anonymous    TINYINT(4)    NOT NULL DEFAULT 0 COMMENT '是否匿名（0=否, 1=是）',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete       TINYINT(4)    NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删, 1=已删）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id) COMMENT '一个订单只能评价一次',
    KEY idx_service_id (service_id),
    KEY idx_buyer_id (buyer_id),
    KEY idx_seller_id (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务评价表';

-- 注意事项：
-- 1. uk_order_id 唯一索引保证每个订单只能评价一次
-- 2. seller_id 冗余存储，方便后续统计卖家被评价情况
-- 3. is_delete 逻辑删除，与项目 MyBatis Plus 配置一致
-- 4. rating 范围 1-5，在业务层校验
