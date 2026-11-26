-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 清空现有数据（可选，防止主键冲突）
TRUNCATE TABLE `service_order`;
TRUNCATE TABLE `service`;
TRUNCATE TABLE `trade_location`;
TRUNCATE TABLE `service_tag`;
TRUNCATE TABLE `user`;

-- 插入测试用户
-- 密码统一为: 123456 (假设加密后为 e10adc3949ba59abbe56e057f20f883e, 注意：实际登录时后端会校验原始密码格式，这里直接插入加密后的密码绕过格式校验，但登录时需使用符合格式的密码如 password123)
-- 为了方便测试，我们将数据库中的密码修改为 'password123' 加密后的值
-- password123 + salt(xueyifang) = xueyifangpassword123 -> MD5 = 97306536605209055424007223006700
INSERT INTO `user` (`id`, `student_id`, `password`, `real_name`, `nickname`, `phone`, `role`, `publish_permission`, `wallet_balance`, `frozen_amount`, `account_status`, `is_delete`, `create_time`, `update_time`) VALUES
(1, '20240001', '534249f67bb81d9808afc189ae7622b0', '管理员', 'Admin', '13800000001', 2, 1, 10000.00, 0.00, 1, 0, NOW(), NOW()),
(2, '20240002', '534249f67bb81d9808afc189ae7622b0', '卖家小王', 'SellerWang', '13800000002', 1, 1, 1000.00, 0.00, 1, 0, NOW(), NOW()),
(3, '20240003', '534249f67bb81d9808afc189ae7622b0', '买家小李', 'BuyerLi', '13800000003', 1, 0, 1000.00, 0.00, 1, 0, NOW(), NOW());

-- 插入服务标签
INSERT INTO `service_tag` (`id`, `tag_name`, `description`, `icon_url`, `is_delete`, `create_time`, `update_time`) VALUES
(1, '跑腿代办', '帮取快递、送文件等', '', 0, NOW(), NOW()),
(2, '学业辅导', '高数、英语、编程辅导', '', 0, NOW(), NOW()),
(3, '闲置交易', '二手书、电子产品', '', 0, NOW(), NOW());

-- 插入交易地点
INSERT INTO `trade_location` (`id`, `location_name`, `location_description`, `location_address`, `is_available`, `is_delete`, `create_time`, `update_time`) VALUES
(1, '图书馆', '图书馆正门', '图书馆', 1, 0, NOW(), NOW()),
(2, '一食堂', '一食堂门口', '一食堂', 1, 0, NOW(), NOW()),
(3, '二教', '第二教学楼大厅', '第二教学楼', 1, 0, NOW(), NOW());

-- 插入服务
-- 服务1: 线上Java辅导 (卖家: 2, 价格: 50, 线上交易)
INSERT INTO `service` (`id`, `service_title`, `service_description`, `seller_id`, `tag_id`, `trade_type`, `price`, `max_purchases`, `current_purchase_count`, `is_physical`, `status`, `review_status`, `view_count`, `collection_count`, `average_rating`, `is_delete`, `create_time`, `update_time`) VALUES
(1, 'Java编程一对一辅导', '资深学长提供Java基础、Spring Boot框架辅导，包教包会', 2, 2, 2, 50.00, -1, 0, 0, 1, 1, 100, 10, 5.0, 0, NOW(), NOW());

-- 服务2: 线下代取快递 (卖家: 2, 价格: 5, 线下交易)
INSERT INTO `service` (`id`, `service_title`, `service_description`, `seller_id`, `tag_id`, `trade_type`, `price`, `max_purchases`, `current_purchase_count`, `is_physical`, `status`, `review_status`, `view_count`, `collection_count`, `average_rating`, `is_delete`, `create_time`, `update_time`) VALUES
(2, '代取快递（小件）', '南门、北门快递代取，送到宿舍楼下', 2, 1, 1, 5.00, 100, 0, 1, 1, 1, 50, 5, 4.8, 0, NOW(), NOW());

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;
