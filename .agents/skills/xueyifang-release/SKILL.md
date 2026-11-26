---
name: xueyifang-release
description: >-
  学艺坊项目的打包发布、部署目录同步与文档/SQL单一真源工作流。
  当用户要求同步答辩部署包（Docker/便携版）、打包发布、同步初始化 SQL、
  或从 Knife4j 导出最新 OpenAPI 接口文档时使用本技能。
---

# 学艺坊打包发布与真源同步工作流

`deploy/docker/` 和 `deploy/portable/` 平时只保留部署模板、`env/` 运行时和脚本，不再手写维护重复的前后端源码副本。每次需要出包或更新部署目录时，通过脚本从主工程实时组装。

## 真源约定（Single Source of Truth）

| 资源 | 唯一真源 | 同步目标 |
|------|----------|----------|
| 后端源码 | `xueyifang-backend/` | 打包时复制干净源码（不含 `target/`、`.git/`、`nul`、运行时日志）并注入对应 profile 配置 |
| 前端源码 | `xueyifang-frontend/` | 打包时复制干净源码（不含 `node_modules/`、`dist/`、`.vite/`、`.git/`） |
| 数据库初始化 SQL | `xueyifang-backend/script/xueyifang-clean-init.sql` | `deploy/docker/init-db/xueyifang-full.sql` 与 `deploy/portable/init-db/xueyifang-full.sql` |
| API 接口文档 | 后端运行中的 Knife4j (`/api/v2/api-docs`) | `deploy/openapi.json`（通过 `--export-openapi` 导出） |

## 常用命令

```powershell
# 1. 组装 Docker 部署包（会自动先跑 unit 层单测与前端构建）
python .agents/skills/xueyifang-release/scripts/package_deploy.py --target docker

# 2. 组装 Windows 便携部署包（保留 env/ 目录不动）
python .agents/skills/xueyifang-release/scripts/package_deploy.py --target portable

# 3. 仅同步数据库初始化 SQL（不复制源码）
python .agents/skills/xueyifang-release/scripts/package_deploy.py --sync-sql

# 4. 清理部署目录中的临时源码副本（恢复为轻量化模板状态）
python .agents/skills/xueyifang-release/scripts/package_deploy.py --clean

# 5. 从运行中的后端（8080）导出最新 OpenAPI JSON 到 deploy/openapi.json
python .agents/skills/xueyifang-release/scripts/package_deploy.py --export-openapi
```

> 若本次刚跑过 `run_qa.py --layer unit`，可加 `--skip-tests` 跳过重复测试。
