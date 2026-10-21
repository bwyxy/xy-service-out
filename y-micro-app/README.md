# y-micro-app

基于 **qiankun + Monorepo** 的微前端项目，使用 **pnpm workspace** 统一管理多应用与本地包。
同一套基座下挂载不同技术栈的子应用，并复用同一批业务包与工具包。

## 技术栈

| 类别       | 技术                            |
|----------|-------------------------------|
| 框架       | Vue 3 + TypeScript            |
| 构建工具     | Vite 7                        |
| 微前端      | qiankun + vite-plugin-qiankun |
| 包管理      | pnpm workspace (monorepo)     |
| 路由       | Vue Router 5                  |
| 状态管理     | Pinia                         |
| UI 组件库   | Element Plus                  |
| CSS 预处理  | Sass                          |
| 桌面端      | Tauri v2                      |
| 即时通讯     | 悟空 IM（easyjssdk / wukongimjssdk） |
| 实时音视频    | LiveKit（livekit-client）       |

## 项目目录

```
y-micro-app/
├── apps/                          # 应用目录（pnpm workspace 包）
│   ├── main/                      # 主应用（基座）
│   ├── y-admin-sub/               # 子应用 - 管理后台
│   ├── y-chat-sub/                # 子应用 - 聊天（含 Tauri 桌面端）
│   └── chat-app/                  # 移动端 - uni-app 工程
├── configs/                       # 公共配置
│   ├── tsconfig.base.json         # 基础 TS 配置
│   └── vite.base.config.ts        # 基础 Vite 配置（公共构建策略）
├── packages/                      # 本地 npm 包
│   ├── xy-qiankun-communication/  # 主应用与子应用通信库
│   └── xy-utils/                  # 通用工具包
├── scripts/                       # 根级脚本
│   ├── dev.js                     # 通用启动脚本（交互式选择应用）
│   └── clean.js                   # 清理脚本
├── Dockerfile.linux               # Tauri Linux 安装包构建镜像
├── pnpm-workspace.yaml            # pnpm 工作空间 + 依赖 catalog
└── package.json                   # 根项目配置
```

## 应用说明

| 应用            | 端口   | 访问路径    | 说明                                        |
|---------------|------|---------|-------------------------------------------|
| main          | 5000 | `/`     | 主应用（基座），注册并加载子应用                          |
| y-chat-sub    | 9001 | `/chat` | 聊天子应用，Element Plus + 悟空 IM，可打包为 Tauri 桌面端 |
| y-admin-sub   | 9002 | `/admin`| 管理后台子应用，Element Plus + ECharts + wangEditor |
| chat-app      | —    | —       | uni-app 移动端工程，非 Vite 构建目标                  |

子应用开发服务器均配置了 `/api` 代理，方便脱离基座独立调试：

| 子应用          | 代理目标                  |
|--------------|-----------------------|
| y-chat-sub   | `http://localhost:9080` |
| y-admin-sub  | `http://localhost:9090` |

## 本地 npm 包

| 包名                          | 说明                                                       |
|-----------------------------|----------------------------------------------------------|
| `@xy/qiankun-communication` | 基于 qiankun `initGlobalState` 封装的主/子应用通信库，提供类型安全的 pub/sub |
| `@xy/utils`                 | 通用工具包：数组、日期、正则、请求、拖拽容器、元素尺寸监听等                           |

本地包通过 `workspace:*` 引用，修改后需重新构建才会生效：

```bash
pnpm build:packages   # 构建全部本地包
```

## 快速开始

### 环境要求

- Node.js >= 20.19
- pnpm >= 10

### 安装依赖

```bash
pnpm install
pnpm install:packages   # 安装本地包依赖
```

### 本地开发

```bash
pnpm dev
```

该命令执行 `scripts/dev.js`，会**交互式列出 `apps/` 下的应用**供选择，并在启动前自动完成：安装目标应用依赖 → 构建本地包 → 执行根级 `predev` 钩子（若存在）。

也可以直接指定应用与模式：

```bash
node scripts/dev.js chat                 # 启动 y-chat-sub
node scripts/dev.js admin --mode production
```

或进入应用目录单独启动：

```bash
cd apps/main && pnpm dev
cd apps/y-chat-sub && pnpm dev
cd apps/y-admin-sub && pnpm dev
```

> 注意：基座与子应用**不能占用同一端口**，否则会出现 base 路径错配、资源 404 等问题。

### 构建

```bash
pnpm build            # 交互式选择应用，production 模式构建
pnpm build:packages   # 仅构建本地包
```

各应用产物输出目录：`main` → `dist`，`y-chat-sub` → `chat`，`y-admin-sub` → `admin`。

### 清理

```bash
pnpm clean                  # 清理 node_modules + dist + tsbuildinfo
node scripts/clean.js --all  # 额外清理 pnpm store 与各类缓存
```

## 环境变量

子应用通过 Vite `loadEnv` 读取，需在**各应用目录下自行创建** `.env.development` / `.env.production`（已被根 `.gitignore` 忽略，不入库）。

| 变量                      | 使用位置            | 说明                             |
|-------------------------|-----------------|--------------------------------|
| `VITE_APP_MICRO_ADMIN`  | main            | admin 子应用入口地址                  |
| `VITE_APP_MICRO_CHAT`   | main            | chat 子应用入口地址                   |
| `VITE_BASE_API_URL`     | 子应用             | 后端网关基础地址                       |
| `VITE_WSS_KEY`          | y-chat-sub      | 登录后写入悟空 IM WSS 地址的 Cookie key  |
| `VITE_NODE_ENV`         | y-chat-sub / y-admin-sub | 控制 qiankun 是否以 dev 模式加载   |

## 微前端架构

### 主应用（基座）

主应用负责：

- 注册和加载子应用（`apps/main/src/qiankun/modules/registerApp.ts`）
- 管理全局状态（`apps/main/src/qiankun/modules/globalState.ts`）
- 提供子应用挂载容器 `#subapp-container`

子应用路由匹配规则：

- `/admin` → 加载 y-admin-sub（entry 取自 `VITE_APP_MICRO_ADMIN`）
- `/chat` → 加载 y-chat-sub（entry 取自 `VITE_APP_MICRO_CHAT`）

### 子应用

每个子应用通过 `vite-plugin-qiankun` 接入 qiankun 生命周期，支持：

- 在基座中作为微应用运行
- 独立运行时直接渲染（开发调试方便）

`y-chat-sub` 的 `base` 为 `/chat`；Tauri 环境下自动切换为相对路径 `./`，产物输出到 `dist`。

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

| 类型                  | 方向          | 说明                |
|---------------------|-------------|-------------------|
| `NAVIGATE`          | 子应用 → 主应用   | 请求基座进行路由跳转        |
| `WARNING_STATUS`    | 主应用 → 子应用   | 推送 WebSocket 报警连接状态 |
| `TOKEN_UPDATE`      | 主应用 → 子应用   | 推送 token 更新       |
| `USER_INFO_UPDATE`  | 主应用 → 子应用   | 推送用户信息更新          |
| `OUT_LOGIN`         | 子应用 → 主应用   | 请求退出登录            |

通信库特性：

- 消息缓存与回放：`on()` 注册时自动回放已收到的同类型消息，避免消息丢失
- 按类型订阅/取消：`on(type, handler)` / `off(type)`
- 生命周期管理：子应用 `unmount` 时自动清理监听器

## 桌面端（Tauri）

桌面端由 `y-chat-sub` 承载（`apps/y-chat-sub/src-tauri`）：

```bash
pnpm desktop:dev           # 桌面端开发调试
pnpm desktop:build:mac     # 构建 macOS 安装包
pnpm desktop:linux         # 通过 Docker 构建 Linux 安装包（deb / rpm / appimage）
```

`pnpm desktop:linux` 会先用 `Dockerfile.linux` 构建镜像，再在容器内执行 `tauri build`，产物输出到 `dist-linux/`。

> 桌面端版本号与 `apps/y-chat-sub/package.json`、`src-tauri/tauri.conf.json` 手工保持一致。

## 依赖管理

项目使用 pnpm workspace 的 `catalog` 功能统一管理依赖版本，配置在 `pnpm-workspace.yaml` 中。

**默认 catalog** —— 引用时使用 `catalog:`：

```json
{
  "dependencies": {
    "vue": "catalog:vue3"
  }
}
```

**具名 catalog** —— 引用时使用 `catalog:<名称>`：

```yaml
# pnpm-workspace.yaml
catalogs:
  vue3:
    vue: ^3.5.40
  vue-router5:
    vue-router: ^5.2.0
```

**本地包** —— 通过 `workspace:*` 引用：

```json
{
  "dependencies": {
    "@xy/qiankun-communication": "workspace:*",
    "@xy/utils": "workspace:*"
  }
}
```

> 根 `package.json` 中配置了 `preinstall: npx only-allow pnpm`，请勿使用 npm / yarn 安装依赖。
