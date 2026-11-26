---
name: xueyifang-dev
description: >-
  学艺坊（Xueyifang）前后端开发脚手架与自检工作流。在需要新增后端接口/业务模块、
  新增前端页面/路由/API、修改数据库结构或字典缓存时使用本技能。
---

# 学艺坊开发与变更自检工作流

在动手写新接口或新页面时，按本清单执行，避免踩进项目历史规范与并发坑。

## 一、新增后端接口 / 模块

1. **Java 8 语法约束**：禁止使用 `var`、`record`、text block、`switch` 表达式；使用 `javax.*` 而非 `jakarta.*`。
2. **分层位置**：
   - `controller/` 接收 `@Valid` DTO，返回 `ResultUtils.success(...)`；需要管理员权限加 `@AuthCheck(mustRole = "admin")`，防重复提交加 `@IdempotentSubmit`，限流加 `@RateLimit`。
   - **公开接口必查**：若无需登录即可访问，必须把路径加入 [WebMvcConfig.java](../../../xueyifang-backend/src/main/java/com/xueyifang/config/WebMvcConfig.java) 的 `excludePathPatterns()`，否则会返回 `40103`。
3. **跨模块依赖**：需要读其他模块的表时，优先注入对方的 `Mapper`，不要在 Service 之间互相注入（禁止靠 `@Lazy` 绕循环依赖）。
4. **并发与资金安全**：
   - 任何修改订单状态/金额的操作：在 `@Transactional(rollbackFor = Exception.class)` 内先调用 `ServiceOrderMapper.selectForUpdate(orderId)` 锁订单行，再校验状态。
   - 修改钱包余额：调用 `UserMapper.selectForUpdate(userId)`；同时涉及两个用户时，**必须按 `userId` 升序加锁**防死锁。
   - 占用/归还服务名额：使用 `ServiceMapper.incrementPurchaseCountIfAvailable` / `decrementPurchaseCount`。
   - 流水号/订单号：统一调用 `SerialNoGenerator.nextOrderNo()` / `nextTransactionNo(userId)`。
   - 异步通知与 WebSocket 推送：包裹在 `TransactionUtils.afterCommit(...)` 内执行，防止事务回滚后发出假通知。
   - 批处理定时任务：外层不加 `@Transactional`，逐条用 `TransactionTemplate` 独立提交并打日志。
5. **统计与列表查询**：列表关联数据批量查询（如 `IServiceImageService.getCoverImageMap`），统计用 SQL 聚合（`SUM` / `COUNT(DISTINCT)` / `GROUP BY`），禁止 `selectList` 拉回内存遍历；`Collectors.toMap` 注意处理可能为 `null` 的 value。
6. **缓存一致性**：Redis 缓存与读取的类型必须一致（PO vs VO 字段名不同会反序列化成 `null`）；写操作完成后删除相关缓存（服务详情、卖家评分、字典等）。
7. **测试**：为新逻辑在 `xueyifang-backend/src/test/` 补充单元测试，用 `mvn test` 验证。

## 二、新增前端页面 / 接口

1. **类型与 API**：先在 `src/types/*.ts` 定义接口类型（与后端 VO/DTO 字段一致），再在 `src/api/*.ts` 封装请求（`request<T>` 已拆包，直接返回 `data`）。
2. **登录态与权限**：
   - 不要在页面或 `api/*.ts` 里手写 401/40103 处理，`request.ts` 已通过 [auth.ts](../../../xueyifang-frontend/src/utils/auth.ts) 统一处理清理与跳转。
   - 新页面在 [router/index.ts](../../../xueyifang-frontend/src/router/index.ts) 配好 `meta`：`requiresAuth` / `requiresAdmin` / `requiresPublishPermission`。
   - 业务错误已在响应拦截器统一 `ElMessage.error`，页面 `catch` 中不要重复弹窗（若需兜底非业务异常，仅在 `typeof error?.code !== 'number'` 时提示）。
3. **复用工具**：
   - 日期与金额格式化统一从 `@/utils/format` 引入，不在组件内重复实现。
   - 状态文案、标签颜色、权限判断统一从 `@/types/*.ts` 引入。
   - 图表通过 `@/utils/echarts` 按需注册引入，不整包 `import * as echarts from 'echarts'`。
   - 修改字典（标签/专业/地点）后，调用 `useDictStore` 的刷新/清缓存方法。

## 三、变更后必跑验证

1. 调用 **`xueyifang-qa`** 技能中的 `run_qa.py --layer unit` 确认后端单测与前端 `vue-tsc` + `vite build` 全部通过。
2. 如变更了数据库表结构，更新 `xueyifang-backend/script/xueyifang-clean-init.sql` 并运行 `package_deploy.py --sync-sql` 同步部署脚本。
