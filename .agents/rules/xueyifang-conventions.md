# 学艺坊（Xueyifang）工程规则

以下规则适用于本仓库所有后续改动：

1. **后端运行在 Java 8 语义下**：严禁使用 `var`、`record`、text block、`switch` 表达式与 `jakarta.*` 包。
2. **公开接口必须注册白名单**：无需登录的接口必须同步加入 `WebMvcConfig.excludePathPatterns()`。
3. **订单与资金并发底线**：修改订单/余额前必须先锁订单行再锁用户行（多用户按 `userId` 升序加锁），库存使用原子 SQL 增减，流水号使用 `SerialNoGenerator`，WebSocket/通知推送通过 `TransactionUtils.afterCommit` 触发。
4. **禁止内存全表统计与 N+1 循环查询**：统计使用 SQL 聚合，列表页关联字段批量查询。
5. **前端公共能力统一复用**：格式化用 `src/utils/format.ts`，登录失效由 `src/utils/auth.ts` + `request.ts` 统一接管，状态枚举复用 `src/types/*.ts`，ECharts 通过 `src/utils/echarts.ts` 按需引入。
6. **测试与发布走工作区 Skill**：日常验证用 `.agents/skills/xueyifang-qa/SKILL.md`，部署包与 SQL 同步用 `.agents/skills/xueyifang-release/SKILL.md`，不再在根目录散放测试脚本或手工维护重复源码副本。
7. **浏览器操作与自动化验收首选 EGO Light (`ego-browser` / `egolite`)**：凡涉及打开网页、前端 UI 验收、跨角色交互测试或截图巡检，一律优先使用 `ego-browser`（如 `ego-browser nodejs`），以获得最高操作效率与最低 Token 消耗。
