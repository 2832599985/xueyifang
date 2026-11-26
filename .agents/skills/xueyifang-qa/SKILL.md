---
name: xueyifang-qa
description: >-
  学艺坊（Xueyifang）项目的分层测试与 AI 协作验收工作流。当用户要求运行测试、验证改动、
  检查接口回归、或对前端页面和跨角色业务流程进行端到端验收时使用本技能。
---

# 学艺坊 QA 与验收工作流

本技能将原本散落在 `test_scripts/` 中的脚本收口为三层可组合、对 AI 友好的工作流，优先用**快速无依赖检查 + 纯 API 集成测试 + AI 浏览器实时验收**替代维护成本高的老式 Playwright CSS 选择器脚本。

## 分层测试策略（由快到慢）

| 层级 | 命令 / 工具 | 依赖环境 | 适用时机 |
|------|-------------|----------|----------|
| **1. 单元测试与编译（离线）** | `python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer unit` | 仅 JDK/Maven + Node | 每次改代码后必跑（秒级） |
| **2. API 集成回归（接口级）** | `python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer api` | MySQL(3306) + Redis(6379) + 后端(8080) | 改动后端接口、订单/钱包/库存/权限逻辑后 |
| **3. UI 与跨角色端到端验收** | **首选 EGO Light (`ego-browser nodejs`)** + [ui-e2e-scenarios.md](./references/ui-e2e-scenarios.md) | 额外启动前端(3000) | 改动前端页面、路由、登录态或需要真实验收时 |

> ⚠️ 在执行第 2、3 层前，先运行 `--check` 确认端口就绪；如需干净的初始库，提示用户或执行：
> `mysql -u root -p < xueyifang-backend/script/xueyifang-clean-init.sql`

## 标准执行步骤

### 步骤 1：环境预检与离线验证

```powershell
# Windows 先初始化 UTF-8 控制台
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false); chcp 65001 > $null

# 1) 跑离线单元测试 + 前端类型检查/构建
python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer unit

# 2) 检查联调端口（3306 MySQL / 6379 Redis / 8080 后端 / 3000 前端）
python .agents/skills/xueyifang-qa/scripts/run_qa.py --check
```

### 步骤 2：运行 API 集成测试（后端已启动时）

`run_qa.py --layer api` 会依次执行 `scripts/` 下的 3 个纯 HTTP 集成套件（不依赖脆弱的浏览器 DOM 选择器）：
1. [trade_integration_test.py](./scripts/trade_integration_test.py) — 登录、充值、发布审核、下单、支付、流水号唯一性、发货、确认收货、评价、待发货/待收货退款、拒绝退款、超卖防护与退款归还库存、聊天、收藏、通知（19 步）
2. [test_admin.py](./scripts/test_admin.py) — 管理后台统计、趋势、权限审核、服务审核、纠纷仲裁、地点/专业管理、系统配置
3. [test_coverage_gaps.py](./scripts/test_coverage_gaps.py) — 认证白名单、未登录拦截（40103）、密码修改/登出失效、边界校验

支持单独跑某一个套件：
```powershell
python .agents/skills/xueyifang-qa/scripts/run_qa.py --suite trade   # 或 admin / gaps
```

### 步骤 3：AI 驱动的 UI / 跨角色浏览器验收（首选 EGO Light / `ego-browser`）

**优先使用 EGO Light (`ego-browser` / `egolite`) 进行自动化浏览器操作与测试（操作效率最高、Token 消耗最低）。** 需要验证前端页面或跨角色交互时：
1. 读取 [ui-e2e-scenarios.md](./references/ui-e2e-scenarios.md) 选择对应场景（P0 交易闭环 / 权限边界 / 退款纠纷 / 资源联动 / 登录过期回跳）。
2. 首选通过 `ego-browser nodejs` 连接 `xueyifang-frontend-qa` taskSpace，在真实 EGO Light 浏览器中批量驱动路由、表单交互、控制台零报错断言与截图。

## 测试账号速查

| 角色 | 学号 | 密码 | 说明 |
|------|------|------|------|
| 管理员 | `20240001` | `password123` | `role=2`，可进 `/admin` |
| 卖家 | `20240002` | `password123` | `publishPermission=1` |
| 买家 | `20240003` | `password123` | `publishPermission=0`，初始余额 10000 |
