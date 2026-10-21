# 终端聊天室 CLI 实现方案

## Context（为什么做）

用户希望有一个「别人用 npm 安装后，终端敲一条命令就能聊天」的终端聊天室。
经确认，不新建聊天协议，而是**复用仓库现有 IM 体系**：终端作为 fly-pigeon（飞鸽）IM 的又一个客户端接入，
与现有网页端（`y-chat-sub`）、桌面端（Tauri）**三方互通**，后端零改动。

已确认的三项决策：
1. 代码位置：**仓库根新增独立目录**（与 `y-micro-app` 微前端工作区解耦）
2. 第一版范围：**极简可聊版**——登录 → 选一个会话 → 实时收发文本消息
3. 设备类型：**PAD(2)**，避开 PC(1) 设备间的多端互踢，可与网页/桌面端同时在线

## 关键技术事实（已调研确认）

| 事项 | 结论 | 证据 |
| --- | --- | --- |
| 登录接口 | `POST {base}/chat/login`，body `{account, password, deviceType, deviceId}` | [AuthController.java](file:///Users/xiaoyi/me-project/xy-service-out/customer-api/fg-chat/src/main/java/com/chat/fg/controller/AuthController.java#L57-L66) |
| 登录响应 | `R{code,msg,data}`，`data = {uid, token, nickname, imAddr:{tcp_addr, ws_addr, wss_addr}}` | [user.ts](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/store/user.ts#L52-L56) |
| 鉴权头 | 请求头名 `token`，值为登录返回的 `token` | 后端 `jwt.header: token`、[request.ts](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/utils/request.ts#L30) |
| 服务地址 | 本地 `http://localhost:9080`（fg-chat 直连，无前缀）；生产地址不入库，由 `XY_CHAT_SERVER` 环境变量或命令行传入 | `.env.production`（已 gitignore） |
| IM 客户端库 | **`easyjssdk`（不是 `wukongimjssdk`）**，官方支持 Node（动态 `require('ws')`） | [wukong-im.ts](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/services/im/wukong-im.ts#L1)、SDK `README.md` 平台表 |
| IM 连接 | `WKIM.init(serverUrl, {uid, token, deviceId?, deviceFlag?}).connect()`；token 放在 JSON-RPC `connect` 参数里 | 同上 `:52-66` |
| 收消息 | `client.on(WKIMEvent.Message, cb)`，事件枚举值 `"message"` | [wukong-im.ts](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/services/im/wukong-im.ts#L170-L188) |
| 发消息 | 走**后端 HTTP** `POST {base}/message/send`，body `{channelId, channelType, type:1, content}` | [chat.ts](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/api/chat.ts#L118-L141) |
| 频道类型 | `Person = 1`、`Group = 2` | `easyjssdk` `ChannelType` |
| 文本消息体 | `{ type: 1, content: '文本' }`（`TEXT = 1`） | [constants.ts](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/services/im/constants.ts#L9) |
| 会话列表 | `POST {base}/conversation/list`（query 参数） | [chat.ts](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/api/chat.ts#L9-L15) |

> 重要设计依据：网页端「**收消息走 SDK、发消息走后端 HTTP**」。终端完全对齐这一模式——
> 发送走后端 HTTP 才能让既有的群成员校验、敏感词过滤、成员变更通知等业务逻辑继续生效。

## 目录与文件结构

新增仓库根目录 `terminal-chat/`（独立 npm 包，自带 `package.json`，不属于 pnpm workspace）：

```
terminal-chat/
├─ package.json          # name/bin/type:module/engines/files
├─ tsconfig.json         # 独立 TS 配置（不继承 y-micro-app 的 base）
├─ tsup.config.ts        # 打包 src/cli.ts → dist/cli.js（ESM + shebang）
├─ README.md             # npx / 全局安装 / 参数说明
└─ src/
   ├─ cli.ts             # 入口（bin）：commander 解析参数、启动 UI
   ├─ types.ts           # 共用类型（R 包装、会话、消息）
   ├─ config.ts          # conf 持久化：serverUrl / token / uid / nickname / deviceId / lastChannel
   ├─ api/
   │  ├─ http.ts         # 原生 fetch 封装：baseURL、token 头、R 解包、401 提示重登
   │  ├─ auth.ts         # login / userinfo / logout
   │  └─ chat.ts         # sendMessage / getConversationList
   ├─ im/client.ts       # easyjssdk 连接、事件订阅、断线指数退避重连
   ├─ core/session.ts    # 编排：登录 → 连 IM → 消息收发 → 内存消息列表
   └─ ui/
      ├─ App.tsx         # 顶层状态机：LoginView → ChatView
      ├─ LoginView.tsx   # 服务器地址 / 账号 / 密码（首次运行）
      ├─ ChannelPicker.tsx # 启动时拉会话列表，方向键选择进入
      ├─ ChatView.tsx    # 消息流 + 输入框 + 底部状态栏（连接状态/当前频道）
      └─ TextInput.tsx   # 基于 useInput 自实现的输入组件（避免额外依赖）
```

## 技术选型

- **运行时**：Node >= 20（原生 `fetch` 可用，零 HTTP 依赖）
- **模块格式**：`"type": "module"`（ESM）。Ink 4+ 为纯 ESM 包，本包必须 ESM
- **TUI**：`ink` + `react`（版本在安装时取当前稳定版）
- **IM**：`easyjssdk` + **必须显式安装 `ws`**（SDK 在 Node 下动态 `require('ws')`，而它只把 `ws` 声明为 optionalDependency）
- **CLI 解析**：`commander`
- **配置持久化**：`conf`（落到 `~/.config/xy-chat-cli/config.json`）
- **构建**：`tsup`（内部用 esbuild，可编译 TSX 并打单文件），开发期用 `tsx` 直跑源码
- **包名 / 命令**：建议 `xy-chat-cli` / bin `xychat`；实施时先用 `npm view <name>` 校验是否被占用，被占用则换名

## 实施步骤

### 第 0 步：连通性验证（spike，必须先做）
写一个临时脚本（验证后删除或不入库），用 `easyjssdk` 在 Node 里走通：
「HTTP 登录拿 token → `WKIM.init().connect()` → 另开网页端发一条消息 → 终端打印出这条消息」。
**风险点**：`easyjssdk` 的 Node 支持只是官方声明 + 代码分支判断，仓库内从未在 Node 跑过。
这一步不通就要改为手写悟空 IM 的 JSON-RPC 协议，方案需重新评估——所以必须最先验证。

### 第 1 步：包骨架与构建链路
`package.json`（`bin: {"xychat": "./dist/cli.js"}`、`files: ["dist"]`、`engines.node >= 20`）、`tsconfig.json`、`tsup.config.ts`。
验证：`pnpm build` 产出 `dist/cli.js`，`node dist/cli.js --help` 能打印帮助。

### 第 2 步：配置与 HTTP 层
`config.ts`（`conf` 实例 + `deviceId` 生成：对 `os.hostname()` 取稳定哈希，保证重启后仍被视为同一台 PAD 设备）。
`api/http.ts`：拼 baseURL、注入 `token` 头、解包 `R{code,msg,data}`、`code !== 200` 抛业务错误、401/4011 提示 token 失效需重新登录。
`api/auth.ts` / `api/chat.ts`：按上表接口签名实现，`deviceType` 固定传 `2`（PAD）。

### 第 3 步：IM 连接层
`im/client.ts` 封装 `easyjssdk`：
- `connect(serverUrl, {uid, token, deviceId, deviceFlag: 1})`（后端配套用 `device_flag=1`）
- 订阅 `Connect/Disconnect/Error/Message` 事件，暴露回调给 `session.ts`
- 断线后指数退避重连（1s → 2s → 4s，上限 30s），状态变更回传给 UI 状态栏

### 第 4 步：会话编排
`core/session.ts`：登录（含 token 复用：已有 token 时先试 `/auth/userinfo`，失效再走登录）→ 连 IM → 启动时调 `/conversation/list` 得会话列表 → 选中频道后把收到的该频道消息推入内存列表、发送时调 `/message/send`。

> `/conversation/list` 返回字段（是否含 `channelName`、`lastMessage`）尚未核实，实施时先读对应 VO 确认；
> 若返回不含会话名称，`ChannelPicker` 退化为「直接输入频道 ID」的简单输入框。

### 第 5 步：Ink UI
- `App.tsx`：状态机 `login → picker → chat`
- `LoginView.tsx`：服务器地址（默认生产地址）、账号、密码；回车提交，错误就地提示
- `ChannelPicker.tsx`：`↑/↓` 选择 + `Enter` 进入
- `ChatView.tsx`：消息流（`fromUid` / 时间 / 内容，自己发的用主题色高亮）+ 底部输入框 + 状态栏（连接状态、当前频道名）
- `TextInput.tsx`：`useInput` 自实现（支持退格、左右移动、粘贴）
- 聊天态命令（输入以 `/` 开头）：
  - `/join <channelId> <group|person>` 切换频道
  - `/msg <uid> [内容]` 快速私聊（切到该 uid 的 Person 频道）
  - `/clear`、`/help`、`/quit`

### 第 6 步：使用体验收尾
- 入口参数（commander）：`--server <url>`、`--channel <id>`、`--type <group|person>`、`--as <account>`、`--logout`
- 环境变量：`XY_CHAT_SERVER`（优先级低于命令行参数）
- README：`npx xy-chat-cli`、`npm i -g xy-chat-cli` 两种用法 + 参数说明

## 明确不做（保持克制）
- 不改任何现有 Java / Vue 代码，不新增后端接口
- 第一版不做：历史消息分页、撤回、@提醒、图片/文件、表情、多会话并行、未读计数、通话
- 不做「发送直连 SDK」的旁路开关（避免绕过服务端业务校验）

## 验证方式

1. **类型与构建**：`pnpm typecheck`（`tsc --noEmit`）+ `pnpm build` 通过
2. **本地端到端**（主验证路径）：
   - 按仓库现有方式启动 fg-chat（端口 9080）
   - 终端 A：`node dist/cli.js --server http://localhost:9080`，用账号 1 登录
   - 终端 B：另一个账号登录，与 A 进入同一群
   - 双向发文本消息，确认**两边都能实时收到**（证明 SDK 收消息 + HTTP 发消息链路都通）
3. **跨端互通**：同时打开网页端（或桌面端）登录同一账号，确认终端发的消息在网页端可见、网页端发的消息终端能收到；且因使用 PAD 设备类型，**网页/桌面端不会被顶下线**
4. **发布形态**：`npm pack` 得到 tgz，`npm i -g ./xy-chat-cli-x.y.z.tgz` 后执行 `xychat --help` 与 `xychat` 正常；再 `npm uninstall -g` 清理
5. **异常路径**：故意填错密码（提示登录失败）、运行中 kill 网络或重启后端（状态栏显示重连中并自动恢复）
