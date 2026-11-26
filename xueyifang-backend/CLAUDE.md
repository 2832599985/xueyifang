# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

学艺坊 (Xueyifang) 后端服务 - 校园技能服务与交易平台，基于 Spring Boot 2.7.2 + MyBatis Plus 3.5.2 + MySQL + Redis。

**Java 版本: 1.8** — 不支持 `var`、record、text block、switch expression 等 Java 10+ 语法。使用 `javax.*`（不是 `jakarta.*`）。

## Build & Run Commands

```bash
# 开发服务器 (端口 8080，上下文路径 /api)
mvn spring-boot:run

# 仅编译（快速检查语法错误）
mvn clean compile

# 跳过测试构建
mvn clean package -DskipTests

# 运行测试
mvn test
```

**API文档**: http://localhost:8080/api/doc.html (Knife4j)

## Architecture

### Technology Stack
- Spring Boot 2.7.2, MyBatis Plus 3.5.2, MySQL, Redis (Lettuce)
- JWT 认证 (jjwt 0.9.0), Knife4j 4.4.0, Hutool 5.8.8
- Apache POI 5.2.3 (用户批量导入), 阿里云 OSS SDK 3.17.1 (可选文件存储)

### Key Patterns

**认证流程**:
- JWT Token 在 `Authorization: Bearer {token}` 请求头
- `JwtAuthInterceptor` 验证 → 用户存入 `UserContext` ThreadLocal → `afterCompletion()` 清理
- 密码使用 MD5 + salt（盐值配置在 `application.yml` 的 `password.salt`）

**统一响应格式**:
```java
// 成功: ResultUtils.success(data)
// 失败: throw new BusinessException(ErrorCode.XXX)
// 响应: { "code": 0, "message": "success", "data": {...} }
```

**权限控制**:
- `@AuthCheck(mustRole = "admin")` 注解控制接口权限
- 角色: 1=学生(普通用户), 2=管理员 (`UserConstant.ROLE_STUDENT`, `ROLE_ADMIN`)

**数据访问**:
- MyBatis Plus + 逻辑删除 (`is_delete` 字段, `@TableLogic`)
- 余额操作使用悲观锁: `UserMapper.selectForUpdate()` + `@Transactional`
- 死锁预防: 涉及两个用户的资金操作按 userId 升序加锁（`ServiceOrderServiceImpl.releaseFundsToSeller()`）
- MyBatis Plus 注册了分页拦截器和乐观锁拦截器（`MyBatisPlusConfig`）

**AOP增强**:
- `@IdempotentSubmit(lockTime=3, message="...")` - 防重复提交 (Redis SETNX, key: `idempotent:{userId}:{class}:{method}`)
- `@RateLimit(key="xxx", maxRequests=10, windowSeconds=60, limitType=USER|IP)` - 接口限流 (Redis sorted set 滑动窗口)
- `@AuthCheck(mustRole="admin")` - 权限校验

### Key Error Codes

| Code | Enum | 说明 |
|------|------|------|
| 40001 | PARAMS_ERROR | 参数错误 (含 @Valid 验证失败) |
| 40003 | NO_AUTH_ERROR | 无权限 |
| 40103 | USER_NOT_LOGIN | 未登录 |
| 40106 | USER_NO_PUBLISH_PERMISSION | 无发布权限 |
| 40201 | SERVICE_NOT_EXIST | 服务不存在 |
| 40301 | ORDER_NOT_EXIST | 订单不存在 |

## Business Rules

### 订单状态机

**订单状态**: 1=待支付, 2=待发货, 3=待收货, 4=已完成, 5=已取消, 6=交易失败

**退款子状态**: 0=无退款 → 1=退款申请中 → 2=卖家同意 / 3=卖家拒绝 → 4=已退款

**支付状态**: 1=未支付, 2=已支付, 3=已退款

**交易类型**: 1=线下(面交), 2=线上担保

**订单号格式**: `ORDER` + yyyyMMddHHmmss + 3位随机数

### 钱包交易类型
1=充值, 2=提现, 3=支付, 4=退款, 5=收入, 6=冻结, 7=解冻（见 `WalletConstant`）

交易号格式: `WT` + yyyyMMddHHmmss + userId

### 定时任务

| 任务 | Cron | 业务规则 |
|------|------|----------|
| `OrderAutoCancelTask` | `0 */10 * * * ?` | 取消超时未支付订单（`ORDER_UNPAID_TIMEOUT_HOURS`） |
| `OrderAutoConfirmReceiptTask` | `0 0 * * * ?` | 自动确认收货（`AUTO_CONFIRM_RECEIPT_DAYS`，跳过退款中的订单） |
| `OrderAutoRefundTask` | `0 30 * * * ?` | 卖家超时未处理退款自动通过（`SELLER_REFUND_TIMEOUT_DAYS`） |

超时阈值存储在 `sys_config` 表中（`SysConfigConstant`）。

### 其他业务规则
- **发布权限**: 用户需管理员审批后才能发布服务
- **REVIEW_MODE 配置**: 1=服务需审核, 2=免审核直接上架
- **专业修改限制**: 用户每30天只能修改一次专业信息
- **注册开关**: `REGISTER_ENABLED` 系统配置控制是否允许注册

## Redis Cache Keys

| Key 模式 | TTL | 用途 |
|----------|-----|------|
| `config:{key}` | 24h | 系统配置 |
| `dict:service_tags` | 12h | 服务标签字典 |
| `dict:professionals` | 24h | 专业列表字典 |
| `dict:trade_locations` | 24h | 交易地点字典 |
| `service:detail:{id}` | 30min | 服务详情缓存 |
| `seller:rating:{id}` | 1h | 卖家评分缓存 |
| `statistics:user:sales:{id}` | 5min | 用户销售统计 |
| `statistics:admin:system` | 10min | 管理员系统统计 |
| `idempotent:{userId}:{class}:{method}` | 默认3s | 幂等锁 |

`CacheWarmUpRunner` 启动时预热字典数据（失败不阻塞启动）。

## Public Endpoint Whitelist

新增不需要登录的 API 必须在 `WebMvcConfig.java` 的 `excludePathPatterns()` 中添加，否则返回 `40103`。

当前业务公开接口: `/auth/login`, `/auth/register`, `/service/tags`, `/professional/list`, `/trade-location/list`, `/review/service/**`, `/file/view/**`, `/sys-config/register-status`, `/ws`

## File Storage

策略模式（`FileStorageStrategy`）支持 local 和 oss 两种实现，由 `file.storage.type` 配置切换（默认 local）。

- 文件路径结构: `{bizType}/{userId}/{yyyyMM}/{uuid-filename}`
- 上传限制: `USER_AVATAR` 最大 1MB, `SERVICE_IMAGE` 最大 5MB（见 `FileUploadBizEnum`）
- 全局限制: `spring.servlet.multipart.max-file-size=10MB`
- 允许格式: jpg, jpeg, png, gif, webp
- OSS 使用时需设置环境变量: `OSS_ACCESS_KEY_ID`, `OSS_ACCESS_KEY_SECRET`

## Configuration

服务端口: **8080**, 上下文路径: **/api**, 活跃 Profile: **dev**

| 配置项 | dev | prod |
|--------|-----|------|
| Redis 密码 | 无 | 有 |
| SQL 日志 | stdout | 关闭 |
| MySQL SSL | 关闭 | 开启 |
| MySQL 字符集 | UTF-8 | utf8mb4 |
| 日志级别 | debug | warn |

JWT 过期: 7天 (604800000ms), Jackson 日期格式: `yyyy-MM-dd HH:mm:ss` (GMT+8)

## Testing

HTTP 测试脚本位于 `script/` 目录，使用 IntelliJ IDEA HTTP Client 执行:
- `钱包模块测试.http`
- `聊天模块测试.http`
- `服务编辑上下架测试.http`
- `管理员纠纷处理与订单自动取消.http`
