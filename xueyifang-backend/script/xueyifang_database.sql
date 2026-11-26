-- =====================================================
-- 《学艺坊》校园技能服务与交易平台 - 数据库初始化脚本
-- 数据库名：xueyifang
-- 字符集：utf8mb4
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS xueyifang CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xueyifang;

-- =====================================================
-- 1. 专业表 - professional
-- =====================================================
CREATE TABLE IF NOT EXISTS professional (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    professional_name VARCHAR(100) NOT NULL UNIQUE COMMENT '专业名称',
    description VARCHAR(500) COMMENT '专业描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT(4) DEFAULT 0 COMMENT '逻辑删除（0=未删除, 1=已删除）',
    INDEX idx_professional_name(professional_name)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';

-- =====================================================
-- 2. 服务标签表 - service_tag
-- =====================================================
CREATE TABLE IF NOT EXISTS service_tag (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    tag_name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    description VARCHAR(200) COMMENT '标签描述',
    icon_url VARCHAR(255) COMMENT '标签图标URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT(4) DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_tag_name(tag_name)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务标签表';

-- =====================================================
-- 3. 交易地点表 - trade_location
-- =====================================================
CREATE TABLE IF NOT EXISTS trade_location (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    location_name VARCHAR(100) NOT NULL COMMENT '地点名称',
    location_description VARCHAR(500) COMMENT '地点描述',
    location_address VARCHAR(200) COMMENT '详细地址',
    is_available TINYINT(4) DEFAULT 1 COMMENT '是否可用（1=可用, 0=不可用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT(4) DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易地点表';

-- =====================================================
-- 4. 用户表 - user（核心表）
-- =====================================================
CREATE TABLE IF NOT EXISTS user (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    student_id VARCHAR(50) NOT NULL UNIQUE COMMENT '学号，唯一，注册后不可修改',
    password VARCHAR(255) NOT NULL COMMENT '密码，加密存储',
    real_name VARCHAR(100) NOT NULL COMMENT '真实姓名',
    nickname VARCHAR(100) COMMENT '昵称',
    phone VARCHAR(20) NOT NULL COMMENT '手机号，必填',
    email VARCHAR(100) COMMENT '邮箱',
    dormitory VARCHAR(100) COMMENT '寝室号',
    grade VARCHAR(20) COMMENT '年级（大一、大二、大三、大四）',
    professional_id BIGINT(20) COMMENT '专业ID',
    avatar VARCHAR(255) COMMENT '头像URL',
    bio VARCHAR(500) COMMENT '个人简介',
    role TINYINT(4) DEFAULT 1 COMMENT '用户角色（1=学生, 2=管理员）',
    publish_permission TINYINT(4) DEFAULT 0 COMMENT '发布权限（0=无, 1=有）',
    permission_review_status TINYINT(4) DEFAULT 0 COMMENT '权限审核状态（0=未审核, 1=已批准, 2=已拒绝）',
    wallet_balance DECIMAL(10,2) DEFAULT 0 COMMENT '钱包余额',
    frozen_amount DECIMAL(10,2) DEFAULT 0 COMMENT '冻结金额（担保交易）',
    account_status TINYINT(4) DEFAULT 1 COMMENT '账户状态（1=正常, 2=禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT(4) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY fk_user_professional(professional_id) REFERENCES professional(id),
    INDEX idx_student_id(student_id),
    INDEX idx_phone(phone),
    INDEX idx_role(role),
    INDEX idx_publish_permission(publish_permission),
    INDEX idx_create_time(create_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 5. 服务表 - service（核心表）
-- =====================================================
CREATE TABLE IF NOT EXISTS service (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    service_title VARCHAR(255) NOT NULL COMMENT '服务标题',
    service_description TEXT NOT NULL COMMENT '服务详情描述',
    seller_id BIGINT(20) NOT NULL COMMENT '发布者ID',
    tag_id BIGINT(20) COMMENT '标签ID',
    trade_type TINYINT(4) NOT NULL COMMENT '交易类型（1=线下交易, 2=线上担保交易）',
    price DECIMAL(10,2) NOT NULL COMMENT '服务价格',
    max_purchases INT(11) DEFAULT -1 COMMENT '最多购买次数（-1=无限制）',
    current_purchase_count INT(11) DEFAULT 0 COMMENT '当前购买次数',
    is_physical TINYINT(4) DEFAULT 0 COMMENT '是否实体物品（1=是, 0=否）',
    status TINYINT(4) DEFAULT 3 COMMENT '服务状态（1=上架, 2=下架, 3=待审核, 4=审核驳回）',
    review_status TINYINT(4) DEFAULT 2 COMMENT '审核状态（1=通过, 2=待审核, 3=驳回）',
    review_reason VARCHAR(500) COMMENT '审核驳回原因',
    reviewed_by BIGINT(20) COMMENT '审核人ID（管理员）',
    review_time DATETIME COMMENT '审核时间',
    view_count INT(11) DEFAULT 0 COMMENT '浏览次数',
    collection_count INT(11) DEFAULT 0 COMMENT '收藏数',
    average_rating DECIMAL(3,2) DEFAULT 0 COMMENT '平均评分',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT(4) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY fk_service_seller(seller_id) REFERENCES user(id),
    FOREIGN KEY fk_service_tag(tag_id) REFERENCES service_tag(id),
    INDEX idx_seller_id(seller_id),
    INDEX idx_status(status),
    INDEX idx_review_status(review_status),
    INDEX idx_tag_id(tag_id),
    INDEX idx_create_time(create_time),
    FULLTEXT INDEX ft_title_description(service_title, service_description)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务表';

-- =====================================================
-- 6. 服务图片表 - service_image
-- =====================================================
CREATE TABLE IF NOT EXISTS service_image (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    service_id BIGINT(20) NOT NULL COMMENT '服务ID',
    image_url VARCHAR(500) NOT NULL COMMENT '图片URL',
    sort_order INT(4) DEFAULT 0 COMMENT '排序顺序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT(4) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY fk_image_service(service_id) REFERENCES service(id),
    INDEX idx_service_id(service_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务图片表';

-- =====================================================
-- 7. 服务订单表 - service_order（核心表）
-- =====================================================
CREATE TABLE IF NOT EXISTS service_order (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_number VARCHAR(100) NOT NULL UNIQUE COMMENT '订单编号',
    service_id BIGINT(20) NOT NULL COMMENT '服务ID',
    buyer_id BIGINT(20) NOT NULL COMMENT '购买者ID',
    seller_id BIGINT(20) NOT NULL COMMENT '卖家ID',
    quantity INT(11) DEFAULT 1 COMMENT '购买数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    trade_type TINYINT(4) NOT NULL COMMENT '交易类型（1=线下交易, 2=线上担保交易）',
    trade_location_id BIGINT(20) COMMENT '交易地点ID（仅线下交易）',
    payment_status TINYINT(4) DEFAULT 1 COMMENT '支付状态（1=未支付, 2=已支付, 3=已退款）',
    order_status TINYINT(4) DEFAULT 1 COMMENT '订单状态（1=待支付, 2=待发货, 3=待收货, 4=已完成, 5=已取消, 6=交易失败）',
    frozen_amount DECIMAL(10,2) DEFAULT 0 COMMENT '冻结金额（担保交易）',
    payment_method TINYINT(4) DEFAULT 1 COMMENT '支付方式（1=钱包余额）',
    payment_time DATETIME COMMENT '支付时间',
    seller_ship_time DATETIME COMMENT '卖家发货时间',
    buyer_confirm_time DATETIME COMMENT '买家确认收货时间',
    refund_status TINYINT(4) DEFAULT 0 COMMENT '退款状态（0=无退款, 1=申请中, 2=卖家同意, 3=卖家拒绝）',
    refund_reason VARCHAR(500) COMMENT '退款原因',
    refund_request_time DATETIME COMMENT '退款申请时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT(4) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY fk_order_service(service_id) REFERENCES service(id),
    FOREIGN KEY fk_order_buyer(buyer_id) REFERENCES user(id),
    FOREIGN KEY fk_order_seller(seller_id) REFERENCES user(id),
    FOREIGN KEY fk_order_location(trade_location_id) REFERENCES trade_location(id),
    INDEX idx_order_number(order_number),
    INDEX idx_buyer_id(buyer_id),
    INDEX idx_seller_id(seller_id),
    INDEX idx_order_status(order_status),
    INDEX idx_payment_status(payment_status),
    INDEX idx_create_time(create_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务订单表';

-- =====================================================
-- 8. 服务纠纷表 - service_dispute
-- =====================================================
CREATE TABLE IF NOT EXISTS service_dispute (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_id BIGINT(20) NOT NULL COMMENT '订单ID',
    dispute_initiator_id BIGINT(20) NOT NULL COMMENT '纠纷发起者ID',
    dispute_type TINYINT(4) NOT NULL COMMENT '纠纷类型（1=卖家未履行, 2=货不对板, 3=质量问题, 4=其他）',
    description TEXT NOT NULL COMMENT '纠纷描述',
    dispute_status TINYINT(4) DEFAULT 1 COMMENT '纠纷状态（1=待审核, 2=处理中, 3=已解决, 4=已驳回）',
    admin_reply TEXT COMMENT '管理员回复',
    resolution VARCHAR(500) COMMENT '解决方案',
    handled_by BIGINT(20) COMMENT '处理人ID（管理员）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT(4) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY fk_dispute_order(order_id) REFERENCES service_order(id),
    FOREIGN KEY fk_dispute_initiator(dispute_initiator_id) REFERENCES user(id),
    FOREIGN KEY fk_dispute_handler(handled_by) REFERENCES user(id),
    INDEX idx_order_id(order_id),
    INDEX idx_dispute_status(dispute_status),
    INDEX idx_create_time(create_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务纠纷表';

-- =====================================================
-- 9. 系统配置表 - sys_config
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value VARCHAR(500) COMMENT '配置值',
    description VARCHAR(500) COMMENT '配置描述',
    is_enabled TINYINT(4) DEFAULT 1 COMMENT '是否启用（1=启用, 0=禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key(config_key)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- =====================================================
-- 10. 管理员设置表 - admin_setting
-- =====================================================
CREATE TABLE IF NOT EXISTS admin_setting (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    admin_id BIGINT(20) NOT NULL COMMENT '管理员ID',
    setting_key VARCHAR(100) NOT NULL COMMENT '设置键',
    setting_value VARCHAR(500) COMMENT '设置值',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY fk_admin_setting_user(admin_id) REFERENCES user(id),
    UNIQUE KEY uk_admin_setting(admin_id, setting_key)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员设置表';

-- =====================================================
-- 11. 用户聊天记录表 - user_chat
-- =====================================================
CREATE TABLE IF NOT EXISTS user_chat (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    sender_id BIGINT(20) NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT(20) NOT NULL COMMENT '接收者ID',
    content TEXT NOT NULL COMMENT '聊天内容',
    message_type TINYINT(4) DEFAULT 1 COMMENT '消息类型（1=文本, 2=图片, 3=文件）',
    is_read TINYINT(4) DEFAULT 0 COMMENT '是否已读（0=未读, 1=已读）',
    related_service_id BIGINT(20) COMMENT '关联服务ID（可选）',
    related_order_id BIGINT(20) COMMENT '关联订单ID（可选）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT(4) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY fk_chat_sender(sender_id) REFERENCES user(id),
    FOREIGN KEY fk_chat_receiver(receiver_id) REFERENCES user(id),
    INDEX idx_sender_receiver(sender_id, receiver_id),
    INDEX idx_is_read(is_read),
    INDEX idx_create_time(create_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户聊天记录表';

-- =====================================================
-- 12. 收藏服务表 - service_favorite
-- =====================================================
CREATE TABLE IF NOT EXISTS service_favorite (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT(20) NOT NULL COMMENT '用户ID',
    service_id BIGINT(20) NOT NULL COMMENT '服务ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY fk_favorite_user(user_id) REFERENCES user(id),
    FOREIGN KEY fk_favorite_service(service_id) REFERENCES service(id),
    UNIQUE KEY uk_user_service(user_id, service_id),
    INDEX idx_user_id(user_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏服务表';

-- =====================================================
-- 13. 通知表 - notification
-- =====================================================
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    recipient_id BIGINT(20) NOT NULL COMMENT '接收者ID',
    notification_type TINYINT(4) NOT NULL COMMENT '通知类型（1=权限批准, 2=服务下架, 3=订单通知, 4=纠纷通知）',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    related_id BIGINT(20) COMMENT '关联ID',
    is_read TINYINT(4) DEFAULT 0 COMMENT '是否已读（0=未读, 1=已读）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY fk_notification_user(recipient_id) REFERENCES user(id),
    INDEX idx_recipient_id(recipient_id),
    INDEX idx_is_read(is_read),
    INDEX idx_create_time(create_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- =====================================================
-- 初始化基础数据
-- =====================================================

-- 插入默认专业
INSERT INTO professional (professional_name, description) VALUES 
('计算机科学与技术', '计算机科学与技术相关专业'),
('软件工程', '软件开发与设计相关专业'),
('信息与计算科学', '信息处理相关专业'),
('电子信息工程', '电子与信息工程相关专业'),
('数据科学与大数据技术', '大数据处理与分析'),
('艺术设计', '视觉设计与艺术设计'),
('工商管理', '商业与管理相关专业'),
('英语', '英语语言与文化'),
('其他', '其他专业');

-- 插入默认服务标签
INSERT INTO service_tag (tag_name, description) VALUES 
('编程开发', '编程、算法、Web开发等技能'),
('平面设计', '海报、UI、LOGO等设计'),
('视频剪辑', '视频编辑、后期处理'),
('翻译服务', '英文、日文等语言翻译'),
('数据分析', '数据分析与可视化'),
('美术绘画', '素描、油画、插画等'),
('文案写作', '文章创意、文案策划'),
('教学辅导', '课程讲解、答疑解惑'),
('生活服务', '其他生活技能服务'),
('手工创意', '手工制作、DIY创意');

-- 插入默认交易地点
INSERT INTO trade_location (location_name, location_description, location_address) VALUES 
('校园咖啡厅', '学生常聚集场所，环境舒适', '校园中心广场旁'),
('图书馆一楼大厅', '人多，安全可靠', '图书馆入口'),
('宿舍楼下', '方便快捷，就近交易', '各宿舍楼下'),
('食堂门口', '人流量大，交易便捷', '学生食堂门口'),
('运动场旁', '开放空间，视野开阔', '学校运动场南侧');

-- 插入系统配置
INSERT INTO sys_config (config_key, config_value, description, is_enabled) VALUES
('REVIEW_MODE', '1', '审核模式：1=全部审核, 2=仅下架后重新申请需审核', 1),
('AUTO_RELEASE_FROZEN_HOURS', '72', '冻结资金自动释放时间（小时）- 已废弃', 0),
('DEFAULT_WALLET_BALANCE', '0', '新注册用户默认钱包余额', 1),
('FROZEN_RELEASE_PERCENTAGE', '100', '交易完成后释放的冻结金额百分比', 1),
('SERVICE_TITLE_MAX_LENGTH', '255', '服务标题最大长度', 1),
('SERVICE_DESC_MAX_LENGTH', '2000', '服务描述最大长度', 1),
('ORDER_UNPAID_TIMEOUT_HOURS', '24', '订单未支付超时自动取消时间（小时）', 1),
('SELLER_REFUND_TIMEOUT_DAYS', '3', '卖家处理退款申请超时时间（天），超时自动退款', 1),
('AUTO_CONFIRM_RECEIPT_DAYS', '7', '发货后自动确认收货时间（天）', 1);

-- =====================================================
-- 脚本执行完成
-- =====================================================
-- 使用本脚本创建数据库后，需要：
-- 1. 修改 application.yml 中的数据库配置（url、username、password）
-- 2. 确保 MySQL 版本 >= 5.7
-- 3. 确保使用 MyBatisPlus 3.5.2 及以上版本
-- 4. 建议使用 Navicat 或 SQLYog 导入本脚本
-- =====================================================