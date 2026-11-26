# 学艺坊 · Xueyifang

> 面向高校校园场景的技能服务交易与平台担保交付系统（全栈商业化 Monorepo）。  
> **Backend**: Spring Boot 2.7 (Java 8) · MyBatis-Plus · MySQL 8 · Redis · WebSocket · S3/OSS  
> **Frontend**: Vue 3 · TypeScript · Vite · Element Plus · Pinia · ECharts

学艺坊为高校学生提供可信的校内技能服务交易闭环（课程辅导、摄影约拍、硬件清灰维修、设计插画、器材陪练、校园跑腿等）。系统以**极简曜石黑 × 纯白**商业视觉重构全端界面，结合**金融级资金担保状态机**、**10 秒防截图动态二维码 + 30 秒周期动态核销码**、**线上交付凭证留痕**与**多维证据仲裁体系**，覆盖从浏览预约、校内当面履约到售后仲裁的完整链路。

---

## 核心特性

### 1. 金融级平台担保与并发一致性
- **完整交易状态机**：`待支付(1) → 待发货(2) → 待收货(3) → 已完成(4)`，覆盖取消(`5`)与退款/仲裁关闭(`6`)全分支。
- **零超卖与零死锁**：支付原子占用库存（`UPDATE ... WHERE count + n <= max`），资金变动前 `SELECT ... FOR UPDATE` 行锁校验，跨账户结算严格按 `userId` 升序加锁防死锁。
- **幂等、限流与防重放**：`@IdempotentSubmit`（Redis SETNX）防重复提交、`@RateLimit`（Redis ZSet 滑动窗口）限流、钱包大额风控护栏（单笔 / 日累计上限）、WebSocket 通知注册在 `TransactionUtils.afterCommit()` 避免事务回滚脏推送。

### 2. 校内当面双模动态核销（10s 防截图二维码 + 30s 动态核销码）
- **无状态 HMAC-SHA256 加密引擎**：下单支付生成单订单高熵密钥种子，密钥永不暴露给前端，服务端基于实时时间窗派生凭证。
- **10 秒防截图动态二维码（`XYF1:<orderId>:<slot>:<HMAC>`）**：
  - 买方端每 10 秒重签刷新并附带实时钟表与倒计时条；超过窗口期直接失效，拦截远程截图转发。
  - 卖方端仅允许调用本机**实时摄像头视频流**（`getUserMedia` + `BarcodeDetector` / `jsQR` 逐帧解码），**严禁相册/本地文件截图导入**，支持销售列表页全局扫码自动匹配订单。
- **30 秒周期动态 6 位核销码**：无摄像头或弱光场景备用方案，结合 Redis 连续输错 **5 次锁定 15 分钟** 防暴力枚举。

### 3. 交付存证、富媒体站内聊天与平台仲裁
- **线上交付凭证**：卖方发货支持填写履约说明并上传交付截图，订单详情页永久留痕。
- **多维纠纷仲裁**：支持买/卖方上传举证截图，发起纠纷自动冻结超时确认收货；管理后台仲裁页内置**买卖双方聊天记录直查**与资金裁决联动。
- **实时通信与校友信任名片**：WebSocket 实时单聊（支持图片与订单卡片）与系统通知；服务详情页展示校内预约时段与实名校友信用档案。

---

## 目录结构

```text
xueyifang/
├── xueyifang-backend/       # Spring Boot 2.7 后端服务（端口 8080，上下文 /api）
│   ├── src/main/java/       # 业务源码、并发锁、TOTP/动态二维码引擎、定时任务与 WebSocket
│   ├── src/main/resources/  # 脱敏默认配置、prod/docker 配置与 application-local.yml.example
│   ├── src/test/java/       # JUnit 5 + Mockito 核心业务与加密引擎单元测试
│   └── script/              # xueyifang-clean-init.sql 一键初始化 SQL 与接口脚本
├── xueyifang-frontend/      # Vue 3 + TypeScript + Vite 前端应用（端口 3000）
│   └── src/                 # 前台市场、订单核销、实时聊天、钱包与独立管理控制台
├── deploy/docker/           # Docker Compose 一键编排模板（MySQL + Redis + Backend + Nginx）
└── .agents/                 # 工程规范与分层自动化 QA 脚本（run_qa.py）
```

---

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
cd deploy/docker
cp .env.example .env
# 编辑 .env，将 MySQL / Redis / JWT_SECRET 替换为强随机密钥
docker compose up -d --build
```

启动后访问：`http://localhost`（Nginx 自动反代 `/api` 与 `/api/ws`）。

### 方式二：本地开发启动

1. **环境要求**：JDK 8+（Java 8 语法，兼容 JDK 17/21）、Maven 3.6+、Node.js 18+、MySQL 8.0、Redis 6+。
2. **初始化数据库**：
   ```bash
   mysql -u root -p < xueyifang-backend/script/xueyifang-clean-init.sql
   ```
3. **配置本地凭据并启动后端**：
   ```bash
   cp xueyifang-backend/src/main/resources/application-local.yml.example \
      xueyifang-backend/src/main/resources/application-local.yml
   # 按需修改 application-local.yml 中的数据库、Redis 或 S3/OSS 参数（该文件已被 .gitignore 忽略）

   cd xueyifang-backend
   mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=local"
   # Knife4j 接口文档：http://localhost:8080/api/doc.html
   ```
4. **启动前端**：
   ```bash
   cd xueyifang-frontend
   npm install
   npm run dev        # http://localhost:3000
   npm run build      # vue-tsc 类型检查 + Vite 生产构建
   ```

### 环境变量一览（生产环境 `--spring.profiles.active=prod`）

| 变量 | 说明 | 默认值（`default`） |
|------|------|------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 地址与库名 | `localhost` / `3306` / `xueyifang` |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 账号密码 | `root` / `changeme`（**生产必填**） |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DB` | Redis 连接配置 | `localhost` / `6379` / 空 / `0` |
| `JWT_SECRET` | JWT 与动态核销码主签名密钥 | 开发占位值（**生产必填**） |
| `FILE_STORAGE_TYPE` | 文件存储引擎：`local` 或 `oss`（兼容 S3/MinIO/SeaweedFS） | `local` |
| `OSS_ENDPOINT` / `OSS_BUCKET_NAME` / `OSS_URL_PREFIX` | S3/OSS 端点与公网前缀 | `http://localhost:9000` |
| `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` | S3/OSS 访问凭据 | 占位值 |

### 初始化演示账号

> 由 `xueyifang-clean-init.sql` 写入，仅供本地演示与自动化测试使用，上线前请务必修改密码或清理。

| 角色 | 学号 | 初始密码 | 说明 |
|------|------|----------|------|
| 管理员 | `20240001` | `password123` | 访问 `/admin` 运营控制台 |
| 卖家 | `20240002` | `password123` | 已开通服务发布权限 |
| 买家 | `20240003` | `password123` | 含初始测试钱包余额 |

---

## 质量门禁与测试

```bash
# 运行后端全部 JUnit 5 单元测试 + 前端 TypeScript 校验与 Vite 生产构建
python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer unit

# 针对运行中的本地后端执行端到端 API 回归测试
python .agents/skills/xueyifang-qa/scripts/run_qa.py --layer api
```
