# 学艺坊 (Xueyifang) v3.0 商业化全栈升级说明

本次升级在不破坏原有悲观锁并发账本、超时自动状态机与 10s/30s 动态核销底座的前提下，并行完成了**商业化数据库与后端重构**、**Web 与运营后台升级**、以及**全新 `xueyifang-mobile`（微信小程序 + Android App 双端工程）**。

## 1. 全栈工程结构

- `xueyifang-backend/` — Spring Boot 2.7 (Java 8) 商业化后端核心
  - `script/v3_commercial_upgrade.sql`：商业化增量迁移脚本（Docker 部署已自动挂载为 `02-v3-commercial.sql`）
  - **多校区与多边形电子围栏**：`CampusController`、`AdminCampusController`、`GeofenceUtils`（奇偶射线法多边形围栏 + Haversine 球面测距）
  - **双向地图选点协商**：`OrderMeetupProposalController`（支持买卖任意一方在地图选点发起提案，对方确认后行锁原子更新订单坐标并记录仲裁日志）
  - **低功耗蓝牙（BLE）双向挑战-应答加密碰面核销**：`OrderBleVerifyController`、`BleMeetupCryptoUtils`（一次性挑战会话、RSSI 近场信号强度过滤、双端角色子密钥 HMAC-SHA256 交叉签名 + 实时 GPS 联合存证）
  - **合规单笔担保支付与平台抽佣分账**：`EscrowPaymentController`（替代无牌照虚拟余额直充，支持微信/支付宝预下单与 HMAC 验签幂等回调；订单完结按 `PLATFORM_COMMISSION_BPS` 自动拆分平台技术服务费并写入 `platform_revenue_ledger`）
  - **真实校园邮箱认证、履约信用分与防跳单风控**：`EduVerifyController`（`.edu.cn` 邮箱 OTP 验证）、`CreditScoreServiceImpl`（履约加分与仲裁败诉扣分）、`ChatAntiBypassUtils`（下单前自动脱敏微信号/手机号并拦截外部收款链接）、`RateLimitAspect`（升级为 Redis ZSet + Lua 原子滑动窗口）
- `xueyifang-frontend/` — Vue 3 + TypeScript Web 市场与运营管理后台
  - 新增 `CampusMapPicker.vue`（校园电子围栏选点地图）与 `MeetupProposalCard.vue`（双向位置确认提案卡片）
  - 新增后台 `/admin/resource/campuses`（多校区电子围栏配置与平台累计抽佣收入看板）
  - 移除详情页硬编码的虚假“实名学籍已核验”与假 5.0 评分，改为真实绑定 `sellerEduVerified` 与 `sellerCreditScore`
- `xueyifang-mobile/` — 全新 `uni-app` (Vue 3 + Pinia + TypeScript) 移动双端工程
  - 一套代码同时编译 **微信小程序 (`mp-weixin`)** 与 **Android 原生安装包 (`app-plus`)**
  - 内置原生地图选点 `uni.chooseLocation`、步行导航 `uni.openLocation`、仅限摄像头的 10s 动态码扫码 `uni.scanCode`，以及零外部依赖的纯 TS **BLE 外围广播/中心扫描双向 HMAC-SHA256 加密核销页 (`pages/verify/ble-meetup.vue`)**

## 2. 本地应用升级与启动步骤

### 方式一：直接在你现有 Git 仓库应用补丁并推送 PR
```bash
git checkout -b feat/commercial-v3
git apply xueyifang-v3-upgrade.patch
# 若本地已有历史 MySQL 库，执行增量升级 SQL：
mysql -u root -p xueyifang < xueyifang-backend/script/v3_commercial_upgrade.sql
```

### 方式二：运行后端、Web 管理端与移动小程序/安卓端
```bash
# 1. 启动后端 (8080)
cd xueyifang-backend && mvn spring-boot:run

# 2. 启动 Web 与管理后台 (3000)
cd xueyifang-frontend && npm install && npm run dev

# 3. 编译微信小程序或运行移动端 H5/Android
cd xueyifang-mobile
npm install
npm run dev:mp-weixin   # 用微信开发者工具打开 dist/dev/mp-weixin
npm run dev:h5          # 浏览器预览移动端 (端口 3001)
```
