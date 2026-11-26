--
-- Host: localhost    Database: xueyifang
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `xueyifang`
--

/*!40000 DROP DATABASE IF EXISTS `xueyifang`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `xueyifang` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `xueyifang`;

--
-- Table structure for table `admin_setting`
--

DROP TABLE IF EXISTS `admin_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_id` bigint NOT NULL COMMENT '管理员ID',
  `setting_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设置键',
  `setting_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设置值',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_setting` (`admin_id`,`setting_key`),
  CONSTRAINT `admin_setting_ibfk_1` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员设置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_setting`
--

LOCK TABLES `admin_setting` WRITE;
/*!40000 ALTER TABLE `admin_setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `admin_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `recipient_id` bigint NOT NULL COMMENT '接收者ID',
  `notification_type` tinyint NOT NULL COMMENT '通知类型（1=权限批准, 2=服务下架, 3=订单通知, 4=纠纷通知）',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知内容',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读（0=未读, 1=已读）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_recipient_id` (`recipient_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `notification_ibfk_1` FOREIGN KEY (`recipient_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification`
--

LOCK TABLES `notification` WRITE;
/*!40000 ALTER TABLE `notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `professional`
--

DROP TABLE IF EXISTS `professional`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `professional` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `professional_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除（0=未删除, 1=已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `professional_name` (`professional_name`),
  KEY `idx_professional_name` (`professional_name`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `professional`
--

LOCK TABLES `professional` WRITE;
/*!40000 ALTER TABLE `professional` DISABLE KEYS */;
INSERT INTO `professional` VALUES (1,'计算机科学与技术','计算机科学与技术相关专业','2025-11-15 14:31:29','2025-11-15 14:31:29',0),(2,'软件工程','软件开发与设计相关专业','2025-11-15 14:31:29','2025-11-15 14:31:29',0),(3,'信息与计算科学','信息处理相关专业','2025-11-15 14:31:29','2025-11-15 14:31:29',0),(4,'电子信息工程','电子与信息工程相关专业','2025-11-15 14:31:29','2025-11-15 14:31:29',0),(5,'数据科学与大数据技术','大数据处理与分析','2025-11-15 14:31:29','2025-11-15 14:31:29',0),(6,'艺术设计','视觉设计与艺术设计','2025-11-15 14:31:29','2025-11-15 14:31:29',0),(7,'工商管理','商业与管理相关专业','2025-11-15 14:31:29','2025-11-15 14:31:29',0),(8,'英语','英语语言与文化','2025-11-15 14:31:29','2025-11-15 14:31:29',0),(9,'其他','其他专业','2025-11-15 14:31:29','2025-11-15 14:31:29',0);
/*!40000 ALTER TABLE `professional` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service`
--

DROP TABLE IF EXISTS `service`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `service_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务标题',
  `service_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务详情描述',
  `seller_id` bigint NOT NULL COMMENT '发布者ID',
  `tag_id` bigint DEFAULT NULL COMMENT '标签ID',
  `trade_type` tinyint NOT NULL COMMENT '交易类型（1=线下交易, 2=线上担保交易）',
  `trade_location_id` bigint DEFAULT NULL COMMENT '交易地点ID（仅线下交易）',
  `price` decimal(10,2) NOT NULL COMMENT '服务价格',
  `max_purchases` int DEFAULT '-1' COMMENT '最多购买次数（-1=无限制）',
  `current_purchase_count` int DEFAULT '0' COMMENT '当前购买次数',
  `is_physical` tinyint DEFAULT '0' COMMENT '是否实体物品（1=是, 0=否）',
  `status` tinyint DEFAULT '3' COMMENT '服务状态（1=上架, 2=下架, 3=待审核, 4=审核驳回）',
  `review_status` tinyint DEFAULT '2' COMMENT '审核状态（1=通过, 2=待审核, 3=驳回）',
  `review_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核驳回原因',
  `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID（管理员）',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `collection_count` int DEFAULT '0' COMMENT '收藏数',
  `average_rating` decimal(3,2) DEFAULT '0.00' COMMENT '平均评分',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_status` (`status`),
  KEY `idx_review_status` (`review_status`),
  KEY `idx_tag_id` (`tag_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_trade_location_id` (`trade_location_id`),
  FULLTEXT KEY `ft_title_description` (`service_title`,`service_description`),
  CONSTRAINT `service_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `user` (`id`),
  CONSTRAINT `service_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `service_tag` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service`
--

LOCK TABLES `service` WRITE;
/*!40000 ALTER TABLE `service` DISABLE KEYS */;
/*!40000 ALTER TABLE `service` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_dispute`
--

DROP TABLE IF EXISTS `service_dispute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_dispute` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `dispute_initiator_id` bigint NOT NULL COMMENT '纠纷发起者ID',
  `dispute_type` tinyint NOT NULL COMMENT '纠纷类型（1=卖家未履行, 2=货不对板, 3=质量问题, 4=其他）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '纠纷描述',
  `evidence_images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '纠纷举证图片（逗号分隔相对路径）',
  `dispute_status` tinyint DEFAULT '1' COMMENT '纠纷状态（1=待审核, 2=处理中, 3=已解决, 4=已驳回）',
  `admin_reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '管理员回复',
  `resolution` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '解决方案',
  `handled_by` bigint DEFAULT NULL COMMENT '处理人ID（管理员）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `fk_dispute_initiator` (`dispute_initiator_id`),
  KEY `fk_dispute_handler` (`handled_by`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_dispute_status` (`dispute_status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `service_dispute_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `service_order` (`id`),
  CONSTRAINT `service_dispute_ibfk_2` FOREIGN KEY (`dispute_initiator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `service_dispute_ibfk_3` FOREIGN KEY (`handled_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务纠纷表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_dispute`
--

LOCK TABLES `service_dispute` WRITE;
/*!40000 ALTER TABLE `service_dispute` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_dispute` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_favorite`
--

DROP TABLE IF EXISTS `service_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `service_id` bigint NOT NULL COMMENT '服务ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_service` (`user_id`,`service_id`),
  KEY `fk_favorite_service` (`service_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `service_favorite_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `service_favorite_ibfk_2` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏服务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_favorite`
--

LOCK TABLES `service_favorite` WRITE;
/*!40000 ALTER TABLE `service_favorite` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_favorite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_image`
--

DROP TABLE IF EXISTS `service_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `service_id` bigint NOT NULL COMMENT '服务ID',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片URL',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_service_id` (`service_id`),
  CONSTRAINT `service_image_ibfk_1` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_image`
--

LOCK TABLES `service_image` WRITE;
/*!40000 ALTER TABLE `service_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_order`
--

DROP TABLE IF EXISTS `service_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单编号',
  `service_id` bigint NOT NULL COMMENT '服务ID',
  `buyer_id` bigint NOT NULL COMMENT '购买者ID',
  `seller_id` bigint NOT NULL COMMENT '卖家ID',
  `quantity` int DEFAULT '1' COMMENT '购买数量',
  `unit_price` decimal(10,2) NOT NULL COMMENT '单价',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `trade_type` tinyint NOT NULL COMMENT '交易类型（1=线下交易, 2=线上担保交易）',
  `trade_location_id` bigint DEFAULT NULL COMMENT '交易地点ID（仅线下交易）',
  `payment_status` tinyint DEFAULT '1' COMMENT '支付状态（1=未支付, 2=已支付, 3=已退款）',
  `order_status` tinyint DEFAULT '1' COMMENT '订单状态（1=待支付, 2=已支付待确认, 3=交易中, 4=已完成, 5=已取消, 6=交易失败）',
  `frozen_amount` decimal(10,2) DEFAULT '0.00' COMMENT '冻结金额（线上交易）',
  `payment_method` tinyint DEFAULT '1' COMMENT '支付方式（1=钱包余额）',
  `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
  `seller_ship_time` datetime DEFAULT NULL COMMENT '卖家发货时间',
  `buyer_confirm_time` datetime DEFAULT NULL COMMENT '买家确认收货时间',
  `refund_status` tinyint DEFAULT '0' COMMENT '退款状态（0=无退款, 1=申请中, 2=卖家同意, 3=卖家拒绝）',
  `refund_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退款原因',
  `refund_request_time` datetime DEFAULT NULL COMMENT '退款申请时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `delivery_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '卖家发货/履约交付说明',
  `delivery_images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '卖家交付成果凭证图片（逗号分隔相对路径）',
  `verify_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '线下交易6位当面核销码',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_number` (`order_number`),
  KEY `fk_order_service` (`service_id`),
  KEY `fk_order_location` (`trade_location_id`),
  KEY `idx_order_number` (`order_number`),
  KEY `idx_buyer_id` (`buyer_id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_payment_status` (`payment_status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_refund_status` (`refund_status`),
  CONSTRAINT `service_order_ibfk_1` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`),
  CONSTRAINT `service_order_ibfk_2` FOREIGN KEY (`buyer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `service_order_ibfk_3` FOREIGN KEY (`seller_id`) REFERENCES `user` (`id`),
  CONSTRAINT `service_order_ibfk_4` FOREIGN KEY (`trade_location_id`) REFERENCES `trade_location` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_order`
--

LOCK TABLES `service_order` WRITE;
/*!40000 ALTER TABLE `service_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_order_log`
--

DROP TABLE IF EXISTS `service_order_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_order_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `old_status` tinyint DEFAULT NULL COMMENT '变更前订单状态',
  `new_status` tinyint DEFAULT NULL COMMENT '变更后订单状态',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID（为空表示系统自动）',
  `operator_role` tinyint DEFAULT NULL COMMENT '操作人角色（1=买家,2=卖家,3=管理员,4=系统）',
  `action_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作类型（PAY/BUYER_CONFIRM/SELLER_CONFIRM/AUTO_RELEASE/REFUND/CANCEL/DISPUTE_CREATE等）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态/资金变更日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_order_log`
--

LOCK TABLES `service_order_log` WRITE;
/*!40000 ALTER TABLE `service_order_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_order_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_review`
--

DROP TABLE IF EXISTS `service_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `service_id` bigint NOT NULL COMMENT '服务ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `buyer_id` bigint NOT NULL COMMENT '评价人ID（买家）',
  `seller_id` bigint NOT NULL COMMENT '卖家ID（冗余，方便统计）',
  `rating` tinyint NOT NULL COMMENT '评分（1-5）',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评价内容（必填，10-500字）',
  `is_anonymous` tinyint NOT NULL DEFAULT '0' COMMENT '是否匿名（0=否, 1=是）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0=未删, 1=已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`) COMMENT '一个订单只能评价一次',
  KEY `idx_service_id` (`service_id`),
  KEY `idx_buyer_id` (`buyer_id`),
  KEY `idx_seller_id` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务评价表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_review`
--

LOCK TABLES `service_review` WRITE;
/*!40000 ALTER TABLE `service_review` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_tag`
--

DROP TABLE IF EXISTS `service_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签描述',
  `icon_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签图标URL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `tag_name` (`tag_name`),
  KEY `idx_tag_name` (`tag_name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_tag`
--

LOCK TABLES `service_tag` WRITE;
/*!40000 ALTER TABLE `service_tag` DISABLE KEYS */;
INSERT INTO `service_tag` VALUES (1,'跑腿代办','帮取快递、送文件等','','2025-11-21 14:26:38','2025-11-21 14:26:38',0),(2,'学业辅导','高数、英语、编程辅导','','2025-11-21 14:26:38','2025-11-21 14:26:38',0),(3,'闲置交易','二手书、电子产品','','2025-11-21 14:26:38','2025-11-21 14:26:38',0);
/*!40000 ALTER TABLE `service_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置值',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置描述',
  `is_enabled` tinyint DEFAULT '1' COMMENT '是否启用（1=启用, 0=禁用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  KEY `idx_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` VALUES (1,'REVIEW_MODE','1','服务发布审核模式（1=需管理员审核后上架, 2=免审直通上架）',1,'2025-11-15 14:31:29','2026-02-19 13:39:31'),(2,'AUTO_RELEASE_FROZEN_HOURS','72','买家确认后，超过此小时数未卖家确认，系统将自动释放冻结资金（历史配置项）',1,'2025-11-15 14:31:29','2025-11-29 15:28:27'),(3,'DEFAULT_WALLET_BALANCE','0','新用户初始默认钱包余额（元）',1,'2025-11-15 14:31:29','2025-11-29 15:46:53'),(4,'FROZEN_RELEASE_PERCENTAGE','100','交易完成结算时，释放给卖家的担保冻结资金比例（0-100%）',1,'2025-11-15 14:31:29','2025-11-23 15:08:04'),(5,'SERVICE_TITLE_MAX_LENGTH','255','服务标题最大字符长度',1,'2025-11-15 14:31:29','2025-11-15 14:31:29'),(6,'SERVICE_DESC_MAX_LENGTH','2000','服务详情描述最大字符长度',1,'2025-11-15 14:31:29','2025-11-15 14:31:29'),(11,'ORDER_UNPAID_TIMEOUT_HOURS','24','未支付订单超时自动取消时间（小时）',1,'2025-11-23 15:08:04','2025-11-23 15:08:04'),(15,'SELLER_REFUND_TIMEOUT_DAYS','3','卖家处理退款申请超时时间（天），超时自动退款',1,'2025-11-28 05:23:31','2025-11-28 05:23:31'),(16,'AUTO_CONFIRM_RECEIPT_DAYS','7','卖家交付后买家超时自动确认收货时间（天）',1,'2025-11-28 05:23:31','2025-11-28 05:23:31'),(17,'REGISTER_ENABLED','1','自助注册开关（1=开启注册, 0=关闭注册）',1,'2026-01-21 16:24:42','2026-02-19 14:23:45'),(18,'WALLET_SINGLE_RECHARGE_LIMIT','5000','钱包单笔充值风控上限（元）',1,'2026-11-25 00:00:00','2026-11-25 00:00:00'),(19,'WALLET_DAILY_RECHARGE_LIMIT','20000','钱包单日累计充值风控上限（元）',1,'2026-11-25 00:00:00','2026-11-25 00:00:00');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trade_location`
--

DROP TABLE IF EXISTS `trade_location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trade_location` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `location_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '地点名称',
  `location_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地点描述',
  `location_address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址',
  `is_available` tinyint DEFAULT '1' COMMENT '是否可用（1=可用, 0=不可用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易地点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trade_location`
--

LOCK TABLES `trade_location` WRITE;
/*!40000 ALTER TABLE `trade_location` DISABLE KEYS */;
INSERT INTO `trade_location` VALUES (1,'图书馆','图书馆正门','图书馆',1,'2025-11-21 14:26:38','2025-11-21 14:26:38',0),(2,'一食堂','一食堂门口','一食堂',1,'2025-11-21 14:26:38','2025-11-21 14:26:38',0),(3,'二教','第二教学楼大厅','第二教学楼',1,'2025-11-21 14:26:38','2025-11-21 14:26:38',0);
/*!40000 ALTER TABLE `trade_location` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号，唯一，注册后不可修改',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码，加密存储',
  `real_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '真实姓名',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号，必填',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `dormitory` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '寝室号',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '年级（大一、大二、大三、大四）',
  `professional_id` bigint DEFAULT NULL COMMENT '专业ID',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '个人简介',
  `role` tinyint DEFAULT '1' COMMENT '用户角色（1=学生, 2=管理员）',
  `publish_permission` tinyint DEFAULT '0' COMMENT '发布权限（0=无, 1=有）',
  `permission_review_status` tinyint DEFAULT NULL COMMENT '权限审核状态（NULL=未申请, 0=审核中, 1=已批准, 2=已拒绝）',
  `permission_apply_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布权限申请理由',
  `wallet_balance` decimal(10,2) DEFAULT '0.00' COMMENT '钱包余额',
  `frozen_amount` decimal(10,2) DEFAULT '0.00' COMMENT '冻结金额（担保交易）',
  `account_status` tinyint DEFAULT '1' COMMENT '账户状态（1=正常, 2=禁用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `student_id` (`student_id`),
  KEY `fk_user_professional` (`professional_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_role` (`role`),
  KEY `idx_publish_permission` (`publish_permission`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`professional_id`) REFERENCES `professional` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'20240001','534249f67bb81d9808afc189ae7622b0','管理员','Admin','13800000001',NULL,NULL,NULL,NULL,NULL,NULL,2,0,2,NULL,10000.00,0.00,1,'2025-11-21 14:26:38','2025-11-25 15:13:05',0),(2,'20240002','534249f67bb81d9808afc189ae7622b0','卖家小王','SellerLi','13800000002','23232@qq.com',NULL,'大三',6,NULL,NULL,1,1,1,'擅长Java与艺术设计课程辅导',10000.00,0.00,1,'2025-11-21 14:26:38','2026-04-16 23:44:22',0),(3,'20240003','534249f67bb81d9808afc189ae7622b0','买家小李','买家小李','13800000003','2832599985@qq.com','3号楼201','大四',2,NULL,NULL,1,1,1,'提供软件工程课程辅导与跑腿代办',10000.00,0.00,1,'2025-11-21 14:26:38','2026-04-16 23:44:22',0);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_chat`
--

DROP TABLE IF EXISTS `user_chat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '聊天内容',
  `message_type` tinyint DEFAULT '1' COMMENT '消息类型（1=文本, 2=图片, 3=文件）',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读（0=未读, 1=已读）',
  `related_service_id` bigint DEFAULT NULL COMMENT '关联服务ID（可选）',
  `related_order_id` bigint DEFAULT NULL COMMENT '关联订单ID（可选）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `fk_chat_receiver` (`receiver_id`),
  KEY `idx_sender_receiver` (`sender_id`,`receiver_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `user_chat_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`),
  CONSTRAINT `user_chat_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户聊天记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_chat`
--

LOCK TABLES `user_chat` WRITE;
/*!40000 ALTER TABLE `user_chat` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_chat` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wallet_transaction`
--

DROP TABLE IF EXISTS `wallet_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `transaction_type` tinyint NOT NULL COMMENT '交易类型（1=充值, 2=提现, 3=支付, 4=退款, 5=收入, 6=冻结, 7=解冻）',
  `amount` decimal(10,2) NOT NULL COMMENT '交易金额（正数）',
  `balance_before` decimal(10,2) NOT NULL COMMENT '交易前可用余额',
  `balance_after` decimal(10,2) NOT NULL COMMENT '交易后可用余额',
  `frozen_before` decimal(10,2) DEFAULT '0.00' COMMENT '交易前冻结金额',
  `frozen_after` decimal(10,2) DEFAULT '0.00' COMMENT '交易后冻结金额',
  `related_order_id` bigint DEFAULT NULL COMMENT '关联订单ID（可选）',
  `transaction_no` varchar(64) DEFAULT NULL COMMENT '流水号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`) COMMENT '用户ID索引',
  KEY `idx_transaction_type` (`transaction_type`) COMMENT '交易类型索引',
  KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引',
  KEY `fk_wallet_order` (`related_order_id`),
  CONSTRAINT `wallet_transaction_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `wallet_transaction_ibfk_2` FOREIGN KEY (`related_order_id`) REFERENCES `service_order` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallet_transaction`
--

LOCK TABLES `wallet_transaction` WRITE;
/*!40000 ALTER TABLE `wallet_transaction` DISABLE KEYS */;
/*!40000 ALTER TABLE `wallet_transaction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'xueyifang'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-16 23:47:37
