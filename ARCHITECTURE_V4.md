# 学艺坊 (Xueyifang) v4.0 全纵深商业产品架构说明

在 v3.0（多校区电子围栏、BLE 近场加密核销、合规单笔担保分账、校园邮箱 OTP、防跳单风控、移动双端）与 v3.1（真实高德瓦片地图打点、10m~2000m 可自由配置米级核销半径前后端强校验、Web 校园邮箱与信用账本）基础上，v4.0 进一步向业务纵深与信任体系纵深演进：

## 1. v4.0 新增四大纵深能力

1. **C2B 校内反向悬赏需求与创作者竞标引擎（`DemandController` / `DemandServiceImpl`）**
   - 解决校园场景大量“非标急单”痛点（考前一对一突击、嵌入式硬件联调、答辩 PPT 精修、跨校区活动跟拍）。
   - 买方发布悬赏时绑定真实校园地图坐标与自定义米级核销半径（`10m ~ 2000m`），服务端强校验多边形围栏；认证创作者（`publishPermission = 1` 且信用分达标）在线提交报价金额、承诺耗时与履约方案。
   - **选标原子转担保单闭环**：发布者点击采纳方案时，后端通过 `SELECT ... FOR UPDATE` 悲观行锁锁定悬赏单，自动创建限购 1 份的定向服务快照并原子生成标准担保订单（完整继承悬赏锁定的经纬度与米级核销半径），无缝汇入微信/支付宝担保托管、10s 动态二维码 / 30s 动态码 / BLE 核销与平台抽佣分账流水线。
2. **创作者公开数字信用档案与作品集主页（`/creators/:id` / `CreatorProfileController`）**
   - 聚合高校 `.edu.cn` 实名核验状态、履约信用分等级、院系专业、历史完结担保单数、在架公开技能橱窗与已核验订单真实评价，同时对学号、手机号、宿舍号及匿名评价买方执行严格隐私脱敏。
3. **管理后台真实地图可视化多边形围栏打点器与悬赏巡检（`CampusGeofenceList.vue` / `AdminDemandList.vue`）**
   - 管理员无需手写多边形坐标 JSON，直接在高德校园瓦片地图上点击追加围栏边界顶点、一键生成矩形围栏、撤销/清空顶点并实时双向同步；新增全校悬赏需求巡检与违规强制关闭治理台。
4. **微信小程序 + Android 双端（`xueyifang-mobile`）同步演进**
   - 移动端底部导航新增「悬赏大厅」，接入原生地图选点 `uni.chooseLocation` 发布悬赏与扫码核销 GPS 坐标透传。

## 2. 数据库增量脚本执行顺序
1. `xueyifang-backend/script/xueyifang-clean-init.sql`
2. `xueyifang-backend/script/v3_commercial_upgrade.sql`
3. `xueyifang-backend/script/v4_product_depth_upgrade.sql`
（Docker Compose 已自动按 `01` / `02` / `03` 顺序挂载初始化）
