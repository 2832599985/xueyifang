# 学艺坊前端项目

## 项目介绍

学艺坊前端项目，基于 Vue 3 + TypeScript + Vite 开发。

## 技术栈

- Vue 3.3.4
- TypeScript 5.2.2
- Vite 4.4.9
- Element Plus 2.3.14
- Vue Router 4.2.4
- Pinia 2.1.6
- Axios 1.5.0

## 项目结构

```
src/
├── api/                    # API接口
│   ├── auth.ts             # 认证相关API
│   └── service.ts          # 服务相关API
├── assets/                 # 静态资源
├── components/             # 公共组件(待开发)
├── router/                 # 路由配置
│   └── index.ts            # 路由主文件
├── stores/                 # 状态管理
│   └── user.ts             # 用户状态
├── styles/                 # 全局样式
│   └── index.scss          # 全局样式文件
├── types/                  # TypeScript类型定义
│   ├── user.ts             # 用户类型
│   └── service.ts          # 服务类型
├── utils/                  # 工具函数
│   └── request.ts          # Axios封装
├── views/                  # 页面组件
│   ├── home/               # 首页
│   ├── login/              # 登录页
│   ├── register/           # 注册页
│   ├── service/            # 服务相关页面
│   ├── user/               # 个人中心
│   └── admin/              # 管理后台
├── App.vue                 # 根组件
└── main.ts                 # 入口文件
```

## 快速开始

### 1. 安装依赖

```bash
npm install
# 或
pnpm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

### 4. 预览生产版本

```bash
npm run preview
```

## 开发指南

### API请求

所有API请求都通过 `src/utils/request.ts` 封装的 axios 实例发送。

示例:
```typescript
import { login } from '@/api/auth'

const handleLogin = async () => {
  const res = await login({
    studentId: '20240001',
    password: 'password123'
  })
  console.log(res)
}
```

### 状态管理

使用 Pinia 进行状态管理。

示例:
```typescript
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
userStore.setToken('xxx')
```

### 路由守卫

在 `src/router/index.ts` 中配置了路由守卫:
- `requireAuth`: 需要登录
- `requireAdmin`: 需要管理员权限

### TypeScript类型

所有接口类型定义在 `src/types/` 目录下。

## 页面路由

| 路径 | 页面 | 说明 |
|-----|------|------|
| `/home` | 首页 | 展示服务列表 |
| `/login` | 登录页 | 用户登录 |
| `/register` | 注册页 | 用户注册 |
| `/service` | 服务列表 | 浏览所有服务 |
| `/service/:id` | 服务详情 | 服务详细信息 |
| `/user` | 个人中心 | 用户信息、我的服务、订单等 |
| `/admin` | 管理后台 | 权限审核、服务审核、纠纷处理等 |

## 组件开发规范

### 命名规范
- 组件文件名使用 PascalCase
- 组件变量名使用 camelCase

### 代码风格
- 使用 `<script setup lang="ts">` 语法
- 使用组合式API
- 合理拆分组件

### 样式规范
- 使用 `scoped` 限定样式作用域
- 使用 SCSS 编写样式
- 遵循 BEM 命名规范

## 常见问题

### Q1: API请求跨域问题？

A: 已在 `vite.config.ts` 中配置代理，开发环境无需担心跨域

### Q2: Element Plus组件按需导入？

A: 已配置 `unplugin-auto-import` 和 `unplugin-vue-components`，无需手动导入

### Q3: 路径别名 @ 无法识别？

A: 已在 `vite.config.ts` 和 `tsconfig.json` 中配置别名

## 后续开发计划

- [ ] 完善首页布局
- [ ] 开发服务列表页面
- [ ] 开发服务详情页面
- [ ] 开发个人中心页面
- [ ] 开发发布服务页面
- [ ] 开发订单管理页面
- [ ] 开发管理后台页面
- [ ] 实现实时聊天功能

