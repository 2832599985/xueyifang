# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

学艺坊 (Xueyifang) - 校园技能服务与交易平台。前后端分离 monorepo 项目。

## Repository Structure

```
xueyifang2.0/
├── .agents/              # AI 协作工作流（rules + xueyifang-dev / xueyifang-qa / xueyifang-release 技能）
├── xueyifang-backend/    # Spring Boot 后端 (端口 8080, 上下文 /api)，独立 git 仓库
│   └── script/           # 数据库脚本 (xueyifang-clean-init.sql) 和 HTTP 测试
├── xueyifang-frontend/   # Vue 3 + Vite 前端 (端口 3000)，独立 git 仓库
├── deploy/               # Docker 与 Windows 便携部署模板（源码通过 xueyifang-release 按需组装）
└── _archive/             # 历史遗留文件归档（含旧 docs/、答辩素材、旧 test_scripts/），不参与构建
```

> 根目录为统一的前后端全栈 Monorepo 仓库（`2832599985/xueyifang`）。若遇到 `dubious ownership`，使用 `git -c safe.directory=* ...`。本机私有数据库/S3 凭据放在已忽略的 `xueyifang-backend/src/main/resources/application-local.yml` 中，使用 `--spring.profiles.active=local` 启动。

详细的子项目文档见:
- `xueyifang-backend/CLAUDE.md` - 后端架构、API 模式、业务规则
- `xueyifang-frontend/CLAUDE.md` - 前端架构、路由、状态管理

## Build Commands

| 操作 | 后端 (`xueyifang-backend/`) | 前端 (`xueyifang-frontend/`) |
|------|------|------|
| 开发 | `mvn spring-boot:run` | `npm run dev` |
| 构建 | `mvn clean package` | `npm run build` |
| 仅编译 | `mvn clean compile` | - |
| 测试 | `mvn test` | - |
| Lint | - | `npm run lint` |

前端 `npm run build` 会先执行 `vue-tsc --noEmit` 类型检查，TypeScript 错误会导致构建失败。

## Architecture Overview

### 认证流程
1. 用户登录 → 后端返回 JWT Token + UserInfo
2. 前端存储 Token 到 localStorage，UserInfo 到 Pinia store
3. 请求时自动添加 `Authorization: Bearer {token}`
4. 后端 `JwtAuthInterceptor` 验证并注入 `UserContext` (ThreadLocal)

### 权限控制
- **前端路由守卫**: `requiresAuth`, `requiresAdmin`, `requiresPublishPermission`
- **后端注解**: `@AuthCheck(mustRole = "admin")`
- **角色**: 1=学生(普通用户), 2=管理员 (见 `UserConstant.ROLE_STUDENT`, `ROLE_ADMIN`)
- **发布权限**: `publishPermission=1` 才能发布服务

### 订单交易流程
```
买家支付 → 资金冻结(frozen_amount) → 卖家发货 → 买家确认 → 资金释放给卖家
                ↓ (退款)
          待发货: 直接退款
          待收货: 卖家同意 或 超时自动退款
```

**订单状态**: 1=待支付, 2=待发货, 3=待收货, 4=已完成, 5=已取消, 6=交易失败

**退款子状态**: 0=无退款 → 1=退款申请中 → 2=卖家同意/3=卖家拒绝 → 4=已退款

### 定时任务（业务超时阈值）

| 定时任务 | 频率 | 业务规则 |
|----------|------|----------|
| `OrderAutoCancelTask` | 每10分钟 | 取消超时未支付订单（阈值由 `ORDER_UNPAID_TIMEOUT_HOURS` 系统配置控制） |
| `OrderAutoConfirmReceiptTask` | 每小时整点 | 自动确认收货（阈值由 `AUTO_CONFIRM_RECEIPT_DAYS` 控制，跳过退款中的订单） |
| `OrderAutoRefundTask` | 每小时30分 | 卖家超时未处理退款自动通过（阈值由 `SELLER_REFUND_TIMEOUT_DAYS` 控制） |

超时阈值存储在 `sys_config` 表中，通过 `SysConfigConstant` 引用。

### 实时通信
- WebSocket 端点: `/api/ws?token={jwt_token}` (token 通过 URL 参数传递，由 `WebSocketHandshakeInterceptor` 独立验证)
- 消息类型: `NEW_CHAT`, `NEW_NOTIFICATION`, `CONNECTED`, `PONG`

## Key Patterns

### 后端统一响应
```java
// 成功
return ResultUtils.success(data);
// 失败
throw new BusinessException(ErrorCode.PARAMS_ERROR, "具体错误");
// 响应格式: { "code": 0, "message": "success", "data": {...} }
```

### 前端 API 封装
```typescript
// src/api/request.ts baseURL='/api', 自动处理 token 和错误
// 成功响应直接返回 data (不含 code/message)
const user = await authApi.getCurrentUser();  // 直接得到 UserInfo
```

### 前端代理配置 (vite.config.ts)
- `/api/*` → `http://localhost:8080/api/*` (API 请求)
- `/backend/*` → `http://localhost:8080/*` (文件上传等绕过 `/api` 前缀的场景)

### 数据库操作
- MyBatis Plus + 逻辑删除 (`is_delete` 字段, `@TableLogic`)
- **订单状态/资金变更**：先 `ServiceOrderMapper.selectForUpdate()` 锁订单行，再校验状态，再锁用户行（`UserMapper.selectForUpdate()`），全部在 `@Transactional` 内
- **死锁预防**: 涉及两个用户的资金操作（如确认收货释放资金）按 userId 升序加锁（见 `ServiceOrderServiceImpl.releaseFundsToSeller()`）
- **库存**: 支付时 `ServiceMapper.incrementPurchaseCountIfAvailable()` 原子占用，退款时 `decrementPurchaseCount()` 归还
- **流水号/订单号**: 统一使用 `SerialNoGenerator`，禁止手拼时间戳
- **副作用（WebSocket 推送等）**: 用 `TransactionUtils.afterCommit()` 在事务提交后执行
- **定时任务批处理**: 外层不加事务，每条记录用 `TransactionTemplate` 独立事务处理
- **统计**: 用 SQL 聚合（见 `ServiceOrderMapper.countAndSumByStatus/dailyOrderStats`），不要 `selectList` 后在内存累加
- **Service 之间避免循环依赖**: 需要跨模块读数据时优先注入对方的 Mapper，不要用 `@Lazy` 绕过

## Important Constraints

### 后端 Java 版本: 1.8
不支持 `var`、record、text block、switch expression 等 Java 10+ 语法。使用 `javax.*` 包（不是 `jakarta.*`）。

### 公开接口白名单
新增不需要登录的 API 时，必须在 `WebMvcConfig.java` 的 `excludePathPatterns()` 中添加路径，否则会返回 `40103 请先登录`。当前已排除的业务接口：`/auth/login`, `/auth/register`, `/service/list`, `/review/service/**`, `/service/tags`, `/professional/list`, `/trade-location/list`, `/file/view/**`, `/sys-config/register-status`, `/ws`（此外 `GET /service/{id}` 由 `JwtAuthInterceptor` 放行）。

### Redis 缓存与 PO/VO 字段名一致性
`CacheWarmUpRunner` 启动时预热字典数据到 Redis（失败不阻塞启动）。缓存写入和读取必须使用相同的类型（PO 或 VO），否则 JSON 反序列化时字段名不匹配会导致值为 null。例如 `ServiceTag.id` vs `ServiceTagVO.tagId` — 缓存预热必须存 VO 格式，因为 Service 层读取缓存时按 VO 反序列化。

### 前端 dict store 缓存
`useDictStore` 缓存服务标签、专业列表、交易地点。页面组件通过 `dictStore.fetchServiceTags()` 获取数据（已加载则跳过请求）。修改字典数据后需调用 `dictStore.clearCache()` 或 `fetchXxx(true)` 强制刷新。

## Database Setup

MySQL 和 Redis 运行在 WSL Docker 中（MySQL 容器名: `mysql8`，端口 3306；Redis 容器名: `redis7`，端口 6379）。

```bash
# 一键初始化（表结构 + 字典数据 + 测试账号，历史迁移脚本均已合并）
mysql -u root -p < xueyifang-backend/script/xueyifang-clean-init.sql
```

配置文件: `xueyifang-backend/src/main/resources/application.yml`（数据库/Redis/JWT 支持 `DB_*`、`REDIS_*`、`JWT_SECRET` 环境变量覆盖；prod profile 下必须通过环境变量提供）

### 前端约定
- 登录态失效统一由 `src/utils/auth.ts#handleAuthExpired` 处理（request 拦截器自动调用），页面无需自行处理 40103
- 日期/金额格式化使用 `src/utils/format.ts`；状态文案/颜色使用 `src/types/*.ts` 中的 helper，不要在页面里再写映射表
- ECharts 通过 `@/utils/echarts` 按需引入

## Test Accounts

| 角色 | 学号 | 密码 | 说明 |
|------|------|------|------|
| 管理员 | 20240001 | password123 | role=2, 可访问 /admin/* |
| 卖家 | 20240002 | password123 | publishPermission=1, 可发布服务 |
| 买家 | 20240003 | password123 | publishPermission=0, 只能购买 |

## AI 协作工作流（`.agents/`）

- **工程规则**：[.agents/rules/xueyifang-conventions.md](.agents/rules/xueyifang-conventions.md)
- **开发自检**：`xueyifang-dev`（新增接口/页面脚手架检查、白名单、并发锁与缓存规范）
- **分层测试**：`xueyifang-qa`（`run_qa.py --layer unit|api` + `preview_*` UI 验收清单）
- **打包发布**：`xueyifang-release`（`package_deploy.py --target docker|portable`、SQL 同步、OpenAPI 导出）

```bash
# 离线单测 + 前端类型检查/构建
python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer unit

# API 集成回归（需先启动 MySQL/Redis/后端 8080）
python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer api

# 组装部署包（默认先跑 unit 门禁，输出到 deploy/docker 或 deploy/portable）
python .agents/skills/xueyifang-release/scripts/package_deploy.py --target docker
```

## Windows Console Encoding

Windows 环境下运行测试前设置 UTF-8:
```powershell
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
chcp 65001 > $null
```
