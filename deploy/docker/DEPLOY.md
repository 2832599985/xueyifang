# 学艺坊 Docker 部署指南

## 前置条件

- 安装 [Docker](https://docs.docker.com/get-docker/) 和 [Docker Compose](https://docs.docker.com/compose/install/)
- 确保 80、3306、6379 端口未被占用

## 快速部署

### 1. 克隆项目

```bash
git clone https://github.com/2832599985/xueyifang.git
cd xueyifang/deploy/docker
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env` 并修改为高强度随机密码与密钥：

```bash
cp .env.example .env
```

```env
MYSQL_ROOT_PASSWORD=change_me_mysql_root_password
MYSQL_DATABASE=xueyifang
REDIS_PASSWORD=change_me_redis_password
JWT_SECRET=change_me_to_a_long_random_32_char_secret_key
PASSWORD_SALT=xueyifang
FILE_STORAGE_TYPE=local
```

> **注意**: 生产环境部署时请务必修改所有默认密码和密钥，切勿提交真实 `.env`。

### 3. 一键构建并启动所有服务

`docker-compose.yml` 已直接指向 Monorepo 内的 `xueyifang-backend`、`xueyifang-frontend` 与 `xueyifang-backend/script/xueyifang-clean-init.sql`（单一真源）：

```bash
docker compose up --build -d
```

首次构建可能需要 5-10 分钟（下载 Maven 与 npm 依赖）。

### 4. 访问系统

- 前台页面: http://localhost （或 http://服务器IP）
- API 文档: http://localhost/api/doc.html

## 测试账号

| 角色 | 学号 | 密码 | 说明 |
|------|------|------|------|
| 管理员 | 20240001 | password123 | 可访问管理后台 /admin/* |
| 卖家 | 20240002 | password123 | 有发布权限，可发布服务 |
| 买家 | 20240003 | password123 | 普通用户，只能购买 |

## 常用命令

```bash
# 查看所有服务状态
docker compose ps

# 查看后端日志
docker compose logs -f backend

# 查看所有服务日志
docker compose logs -f

# 停止所有服务
docker compose down

# 重启某个服务
docker compose restart backend

# 重新构建并启动（代码更新后）
docker compose up --build -d

# 清除所有数据（包括数据库），重新开始
docker compose down -v
```

## 目录结构

```
xueyifang2.0/
├── docker-compose.yml          # Docker Compose 编排文件
├── .env                        # 环境变量配置
├── nginx/
│   └── default.conf            # Nginx 反向代理配置
├── init-db/
│   └── xueyifang-full.sql      # 数据库初始化脚本（由 package_deploy.py 从 clean-init 同步）
├── xueyifang-backend/
│   ├── Dockerfile              # 后端构建文件
│   └── src/main/resources/
│       └── application-docker.yml  # Docker 环境配置
└── xueyifang-frontend/
    └── Dockerfile              # 前端构建文件
```

## 常见问题

**Q: 后端启动失败，提示连接数据库超时？**
A: MySQL 容器首次启动需要初始化数据库，可能需要 30-60 秒。`docker compose` 已配置健康检查和依赖关系，后端会等待 MySQL 就绪后才启动。如仍有问题，尝试 `docker compose restart backend`。

**Q: 如何修改上传文件的存储位置？**
A: 上传文件存储在 Docker named volume `uploads` 中，映射到容器内 `/var/xueyifang/uploads`。如需挂载到宿主机指定目录，修改 `docker-compose.yml` 中 backend 的 volumes 配置，将 `uploads:/var/xueyifang/uploads` 改为 `./uploads:/var/xueyifang/uploads`。

**Q: 如何更新部署？**
A: 拉取最新代码后执行 `docker compose up --build -d`，会自动重新构建变更的镜像并重启服务。
