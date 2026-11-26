# 学艺坊后端项目

## 项目介绍

学艺坊后端服务，基于 Spring Boot 2.7.2 + MyBatis Plus 3.5.2 开发。

## 技术栈

- Spring Boot 2.7.2
- MyBatis Plus 3.5.2
- MySQL 5.7/8.0
- Redis
- JWT认证
- Knife4j API文档

## 项目结构

```
src/main/java/com/xueyifang/
├── XueyifangBackendApplication.java   # 启动类
├── annotation/      # @AuthCheck 权限校验、@IdempotentSubmit 防重复提交、@RateLimit 限流
├── aop/             # 上述注解的切面实现
├── common/          # BaseResponse / ErrorCode / ResultUtils 等通用类
├── config/          # CORS、Knife4j、MyBatis-Plus、WebMvc、WebSocket、异步线程池、缓存预热
├── constant/        # 订单、服务、钱包、缓存、系统配置等常量
├── controller/      # REST 接口（用户 / 服务 / 订单 / 钱包 / 纠纷 / 聊天 / 管理后台 ...）
├── domain/          # dto（请求）/ po（数据库实体）/ vo（响应）
├── enums/           # 文件存储类型、上传业务类型
├── exception/       # BusinessException + 全局异常处理
├── interceptor/     # JWT 认证拦截器
├── mapper/          # MyBatis-Plus Mapper（含悲观锁、原子库存、统计聚合 SQL）
├── service/         # 业务接口与实现
├── strategy/        # 文件存储策略（本地 / OSS）
├── task/            # 定时任务：超时取消、自动确认收货、超时自动退款
├── utils/           # JwtUtils、RedisUtils、UserContext、SerialNoGenerator、TransactionUtils
└── websocket/       # WebSocket 会话管理与消息推送
```

## 快速开始

### 1. 数据库配置

#### 🚀 推荐：一键导入干净的初始化库（含表结构 + 字典数据 + 3 个测试账号）

```bash
# 一条命令完成数据库创建 + 表结构 + 种子数据
mysql -u root -p < script/xueyifang-clean-init.sql
```

该文件（`script/xueyifang-clean-init.sql`）是从当前运行环境完整 `mysqldump` 导出的干净初始库：

- ✅ 自动 `DROP DATABASE IF EXISTS xueyifang` + `CREATE DATABASE`，无需手动建库
- ✅ 包含全部表结构（所有历史迁移脚本已合并）
- ✅ 预置字典数据（服务标签、专业列表、交易地点、系统配置）
- ✅ 预置 3 个测试账号（均为 10000 余额，见下方 *测试账号* 章节）
- ✅ 业务数据已清空，开箱即用无脏数据

> **建议**：新环境搭建请优先使用此文件，无需再逐个执行 `xueyifang_database.sql` + `service_order_log.sql` + `wallet_transaction.sql` 等旧迁移脚本。

#### 配置连接信息

修改 `xueyifang-backend/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/xueyifang?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true
    username: root
    password: 你的密码
```

#### 测试账号

| 角色 | 学号 | 密码 | 说明 |
|------|------|------|------|
| 管理员 | 20240001 | password123 | role=2，可访问 /admin/* |
| 卖家 | 20240002 | password123 | publishPermission=1，可发布服务 |
| 买家 | 20240003 | password123 | 只能购买，账户余额 10000 |

### 2. Redis配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 你的密码
```

### 3. 启动项目

```bash
mvn clean install
mvn spring-boot:run
```

### 4. 访问API文档

启动成功后访问: http://localhost:8080/api/doc.html

## 开发指南

### 使用 MyBatisX 生成代码

1. 安装 IDEA 插件: MyBatisX
2. 右键数据库表 → MyBatis Generator
3. 选择生成路径，生成 Entity、Mapper、Service

### API开发流程

1. 创建 Controller
2. 添加 `@RestController` 和 `@RequestMapping` 注解
3. 使用 `ResultUtils.success()` 和 `ResultUtils.error()` 返回结果
4. 使用 `@AuthCheck` 注解进行权限校验

### 错误处理

- 抛出 `BusinessException` 异常
- 全局异常处理器会自动捕获并返回统一格式

## API接口规范

### 请求格式

- 使用 JSON 格式
- 需要认证的接口在 Header 中添加 `Authorization: Bearer {token}`

### 响应格式

成功响应:
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败响应:
```json
{
  "code": 40001,
  "message": "错误信息"
}
```

## 常见问题

### Q1: 启动报错找不到数据库？

A: 推荐直接导入 `script/xueyifang-clean-init.sql`（完整干净初始库，含表结构 + 字典数据 + 测试账号）：

```bash
mysql -u root -p < script/xueyifang-clean-init.sql
```

（旧的散落迁移脚本如 `xueyifang_database.sql`、`service_order_log.sql` 等已合并到上述 clean-init 中，不需要再单独执行）

### Q2: Redis连接失败？

A: 请确保 Redis 服务已启动，或者在配置文件中修改 Redis 地址

### Q3: MyBatis Plus 日志不显示？

A: 开发环境已配置 `log-impl: org.apache.ibatis.logging.stdout.StdOutImpl`

## 端到端测试

### 测试结果概览

| 测试模块 | 通过 | 失败 | 通过率 |
|---------|-----|-----|-------|
| 地点管理联动测试 | 4 | 0 | 100% |
| 权限审批联动测试 | 5 | 0 | 100% |
| 完整订单流程测试 | 6 | 0 | 100% |
| 退款流程联动测试 | 6 | 0 | 100% |
| 通知联动测试 | 7 | 0 | 100% |
| 综合跨角色联动测试 | 18 | 0 | 100% |
| **总计** | **46** | **0** | **100%** |

✅ 所有跨角色端到端联动测试通过！

详细测试报告请查看 [TEST_REPORT.md](./TEST_REPORT.md)

### 运行测试

```bash
cd test_scripts
python run_e2e_cross_role.py
```

---

## 开发进度

### ✅ 已完成功能

#### 1. 用户模块 (User Module)
- [x] 用户注册接口 `POST /api/user/register`
- [x] 用户登录接口 `POST /api/user/login`
- [x] 用户登出接口 `POST /api/user/logout`
- [x] 获取当前用户信息 `GET /api/user/current`
- [x] 更新用户信息 `PUT /api/user/update`
- [x] 修改密码 `PUT /api/user/change-password`
- [x] 申请发布权限 `POST /api/user/apply-publish-permission`

#### 2. 专业模块 (Professional Module)
- [x] 查询所有专业 `GET /api/professional/list`
- [x] 查询单个专业 `GET /api/professional/{id}`
- [x] 添加专业 `POST /api/professional/add`
- [x] 更新专业 `PUT /api/professional/update`
- [x] 删除专业 `DELETE /api/professional/{id}`

#### 3. 服务标签模块 (Service Tag Module)
- [x] 获取服务标签列表 `GET /api/service/tags`

#### 4. 服务模块 (Service Module)
- [x] 发布服务 `POST /api/service/publish`
- [x] 查询服务列表 `GET /api/service/list`
- [x] 查询服务详情 `GET /api/service/{id}`
- [x] 更新服务信息 `PUT /api/service/update`
- [x] 上架/下架服务 `PUT /api/service/toggle-status`
- [x] 服务审核 `PUT /api/admin/service/review`

#### 5. 订单模块 (Order Module)
- [x] 创建订单 `POST /api/order/create`
- [x] 查询订单列表 `GET /api/order/list`
- [x] 查询订单详情 `GET /api/order/{id}`
- [x] 取消订单 `PUT /api/order/cancel`
- [x] 确认收货 `PUT /api/order/confirm`
- [x] 订单支付 `POST /api/order/pay`
- [x] 卖家发货 `PUT /api/order/ship`
- [x] 申请退款 `POST /api/order/refund`

#### 6. 钱包模块 (Wallet Module)
- [x] 查询钱包余额 `GET /api/wallet/balance`
- [x] 充值 `POST /api/wallet/recharge`
- [x] 提现 `POST /api/wallet/withdraw`
- [x] 交易记录查询 `GET /api/wallet/transactions`

#### 7. 纠纷模块 (Dispute Module)
- [x] 发起纠纷 `POST /api/dispute/create`
- [x] 查询纠纷列表 `GET /api/dispute/list`
- [x] 查询纠纷详情 `GET /api/dispute/{id}`
- [x] 处理纠纷 `PUT /api/admin/dispute/handle`

#### 8. 管理后台模块 (Admin Module)
- [x] 权限审核 `PUT /api/admin/permission/review`
- [x] 服务审核 `PUT /api/admin/service/review`
- [x] 纠纷处理 `PUT /api/admin/dispute/handle`
- [x] 数据统计 `GET /api/admin/dashboard`
- [x] 地点管理 `GET/POST/PUT/DELETE /api/admin/trade-location`
- [x] 专业管理 `GET/POST/PUT/DELETE /api/admin/professional`

#### 9. 聊天模块 (Chat Module)
- [x] 发送消息 `POST /api/chat/send`
- [x] 获取会话列表 `GET /api/chat/conversations`
- [x] 获取聊天记录 `GET /api/chat/messages`

#### 10. 收藏模块 (Favorite Module)
- [x] 收藏服务 `POST /api/favorite/add`
- [x] 取消收藏 `DELETE /api/favorite/remove`
- [x] 收藏列表 `GET /api/favorite/list`

#### 11. 通知模块 (Notification Module)
- [x] 通知列表 `GET /api/notification/list`
- [x] 标记已读 `PUT /api/notification/read`
- [x] 全部已读 `PUT /api/notification/read-all`

### 📊 开发统计

| 模块 | 进度 | 接口数 | 完成数 |
|------|------|--------|--------|
| 用户模块 | ✅ 100% | 7 | 7 |
| 专业模块 | ✅ 100% | 5 | 5 |
| 服务标签模块 | ✅ 100% | 1 | 1 |
| 服务模块 | ✅ 100% | 6 | 6 |
| 订单模块 | ✅ 100% | 8 | 8 |
| 钱包模块 | ✅ 100% | 4 | 4 |
| 纠纷模块 | ✅ 100% | 4 | 4 |
| 管理后台模块 | ✅ 100% | 6 | 6 |
| 聊天模块 | ✅ 100% | 3 | 3 |
| 收藏模块 | ✅ 100% | 3 | 3 |
| 通知模块 | ✅ 100% | 3 | 3 |
| **总计** | **100%** | **50** | **50** |

### 📝 最近更新

#### 2026-01-24
- ✅ 所有测试100%通过
  - 修复前端路由守卫权限检查
  - 修复测试数据：买家publish_permission重置为0
  - 测试结果: 46/46 (100%)
  - 详细报告见 [TEST_REPORT.md](./TEST_REPORT.md)

- ✅ 完成测试脚本质量修复
  - 消除弱断言和"永远PASS"逻辑
  - 权限测试改为严格验证
  - 修复服务审核页面元素选择器
  - 修复买家购买测试：遍历多个服务卡片

- ✅ 完成跨角色端到端联动测试
  - 创建 6 个测试脚本文件
  - 测试覆盖 P0/P1/P2 优先级场景
  - 生成 55+ 张测试截图

- ✅ 所有功能模块开发完成
  - 用户模块、专业模块、服务标签模块
  - 服务模块、订单模块、钱包模块
  - 纠纷模块、管理后台模块
  - 聊天模块、收藏模块、通知模块
  - 共计 50 个 API 接口

#### 2025-11-24
- ✅ 完成钱包模块
  - 钱包余额查询、充值、提现
  - 交易记录查询（支持分页和筛选）
  - 悲观锁保证并发安全

#### 2025-10-28
- ✅ 完成服务标签模块
- ✅ 完成专业模块
- ✅ 完成用户模块

