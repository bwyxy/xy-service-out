# y-micro-app

基于 **qiankun + Monorepo** 架构的微前端项目，使用 **pnpm workspace** 进行多包管理。
使用qiankun 进行不同技术栈的统一管理

## 技术栈

| 类别      | 技术                            |
|---------|-------------------------------|
| 框架      | Vue 3 + TypeScript            |
| 构建工具    | Vite 7                        |
| 微前端     | qiankun + vite-plugin-qiankun |
| 包管理     | pnpm workspace (monorepo)     |
| 路由      | Vue Router 5                  |
| 状态管理    | Pinia                         |
| UI 组件库  | Element Plus                  |
| CSS 预处理 | Sass                          |

## 项目目录

```
y-micro-app/
├── apps/                          # 应用目录
│   ├── main/                      # 主应用（基座）
│   ├── y-admin-sub/               # 子应用 - 管理后台
│   └── y-chat-sub/                # 子应用 - 聊天
├── configs/                       # 公共配置
│   ├── tsconfig.base.json         # 基础 TS 配置
│   └── vite.base.config.ts        # 基础 Vite 配置
├── packages/                      # 本地 npm 包
│   └── xy-qiankun-communication/  # 主应用与子应用通信库
├── pnpm-workspace.yaml            # pnpm 工作空间配置（统一依赖版本）
└── package.json                   # 根项目配置
```

## 应用说明

| 应用 | 端口 | 说明 |
|------|------|------|
| main | 5000 | 主应用（基座），负责注册和加载子应用 |
| y-chat-sub | 9001 | 聊天子应用，使用 Element Plus |
| y-admin-sub | 9002 | 管理后台子应用 |

## 快速开始

### 环境要求

- Node.js >= 18
- pnpm >= 10

### 安装依赖

```bash
pnpm install
```

### 本地开发

```bash
# 并行启动所有应用
pnpm dev
```

也可以单独启动某个应用：

```bash
# 进入对应应用目录
cd apps/main && pnpm dev
cd apps/y-chat-sub && pnpm dev
cd apps/y-admin-sub && pnpm dev
```

### 构建

```bash
# 构建所有应用
pnpm build:apps

# 构建本地包
pnpm build:packages
```

## 微前端架构

### 主应用（基座）

主应用负责：
- 注册和加载子应用（`apps/main/src/qiankun/modules/registerApp.ts`）
- 管理全局状态（`apps/main/src/qiankun/modules/globalState.ts`）
- 提供子应用挂载容器 `#subapp-container`

子应用路由匹配规则：
- `/admin` → 加载 y-admin-sub
- `/chat` → 加载 y-chat-sub

### 子应用

每个子应用通过 `vite-plugin-qiankun` 接入 qiankun 生命周期，支持：
- 在基座中作为微应用运行
- 独立运行时直接渲染（开发调试方便）

### 应用间通信

`@xy/qiankun-communication` 是基于 qiankun `initGlobalState` 封装的通信库，提供类型安全的 pub/sub 消息机制。

**主应用侧使用 `BaseAppCommunication`：**

```typescript
import { baseComm } from '@/qiankun'

// 订阅子应用消息
baseComm.on(MessageType.NAVIGATE, (msg) => {
  console.log('收到子应用路由跳转请求', msg.payload)
})

// 向子应用发送消息
baseComm.send({
  type: MessageType.TOKEN_UPDATE,
  payload: { token: 'xxx' }
})
```

**子应用侧使用 `MicroAppCommunication`：**

```typescript
import { microComm, registerGlobalListeners } from '@/qiankun'

// 在 mount 生命周期中初始化
registerGlobalListeners(props)

// 订阅主应用消息
microComm.on(MessageType.TOKEN_UPDATE, (msg) => {
  console.log('token 更新', msg.payload.token)
})

// 向主应用发送消息
microComm.send({
  type: MessageType.NAVIGATE,
  payload: { path: '/admin/users' }
})
```

**支持的消息类型（`MessageType`）：**

| 类型 | 方向 | 说明 |
|------|------|------|
| `NAVIGATE` | 子应用 → 主应用 | 请求基座进行路由跳转 |
| `WARNING_STATUS` | 主应用 → 子应用 | 推送 WebSocket 报警连接状态 |
| `TOKEN_UPDATE` | 主应用 → 子应用 | 推送 token 更新 |
| `USER_INFO_UPDATE` | 主应用 → 子应用 | 推送用户信息更新 |
| `OUT_LOGIN` | 子应用 → 主应用 | 请求退出登录 |

通信库特性：
- 消息缓存与回放：`on()` 注册时自动回放已收到的同类型消息，避免消息丢失
- 按类型订阅/取消：`on(type, handler)` / `off(type)`
- 生命周期管理：子应用 `unmount` 时自动清理监听器

## 依赖管理

项目使用 pnpm workspace 的 `catalog` 功能统一管理依赖版本，配置在 `pnpm-workspace.yaml` 中。引用方式：

```yaml
# pnpm-workspace.yaml
catalog:
  vue: ^3.5.40
catalogs:
  vue3:
    vue: ^3.5.40
  vue-router5:
    vue-router: ^5.2.0
```

在子应用中引用
```json
{
  "dependencies": {
    "vue": "catalog:vue3",
    "vue-router": "catalog:vue-router5"
  }
}
```

本地包通过 `workspace:*` 引用：

```json
{
  "dependencies": {
    "@xy/qiankun-communication": "workspace:*"
  }
}
```
