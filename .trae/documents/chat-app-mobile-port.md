# chat 项目移动端迁移实施方案（chat-app / uni-app App）

## Context

现有聊天前端 `y-micro-app/apps/y-chat-sub` 是一套面向 **浏览器 + Tauri 桌面端** 的 Vue3 应用（Element Plus + vue-router + pinia + axios 封装 + js-cookie + easyjssdk 悟空 IM + WebRTC + qiankun 微前端）。

用户希望把它做成 **移动端 App（Android/iOS）**，并已搭好 uni-app 空壳工程 `y-micro-app/apps/chat-app`（HBuilderX 工程，`vueVersion: 3`，已装 wot-design-uni，`pages.json` 中只有 首页/社区/联系人/动态 四个空页面 + tabBar，`page-layout`、`quick-menu` 均为空组件）。

本期目标（已与用户确认）：

- **目标平台**：App（Android/iOS）
- **功能范围**：核心 IM 版 —— 登录注册、会话列表、聊天窗口、联系人/好友、通知
- **Tab 结构**：沿用现有 首页 / 社区 / 联系人 / 动态（社区、动态本期为占位页）

由于两端运行时差异极大（App 逻辑层没有浏览器 API），**不能复用 y-chat-sub 源码**，需要按移动端重写一套轻量实现，但 **HTTP 接口契约、IM 协议、数据模型保持一致**，直接对接现有后端 `chat-api`。

---

## 技术决策（已逐项验证）

| # | 决策 | 依据 |
|---|---|---|
| 1 | uni-app App + Vue3，状态管理直接用 **内置 Pinia**，无需 npm 安装 | uni-app Vue3 内置 pinia 2.1.7；HBuilderX 工程「无需手动安装」 |
| 2 | IM 直接复用 **easyjssdk v2.0.1**，不改其协议代码 | 其 `detectPlatform()` 优先探测 `uni.connectSocket`，并内置 `UniAppWebSocketAdapter`（走 SocketTask 的 onOpen/onMessage/onClose/onError） |
| 3 | SDK 以 **内联文件** 方式引入（`libs/easyjssdk.js`） | chat-app 无 `package.json`/`node_modules`，无法 npm 安装；SDK 的 ESM 产物为**单文件、无顶层 import**（44KB，仅 Node 分支才有懒加载 `require`），可直接复制。源：`node_modules/.pnpm/easyjssdk@2.0.1/node_modules/easyjssdk/dist/esm/index.js` |
| 4 | 启动最先注入 **Base64/UTF-8 polyfill**，不改 SDK | SDK 的 `_encodePayloadToBase64` 只认 `TextEncoder+btoa` / `Buffer` / `wx.*`；`_decodeBase64ToStr` 只认 `atob+TextDecoder` / `Buffer` / `wx.*`；适配器 `onMessage` 直接用 `new TextDecoder()`。App 逻辑层这些 API 不可靠 → 缺失时挂纯 JS 实现到 `globalThis` |
| 5 | HTTP 用 **`uni.request` 封装**替代 axios | `@xy/utils` 的 `createAxios` 依赖浏览器 XHR。新封装保持 **返回 `{code,msg,data}` 信封**，使 api 层调用点 1:1 迁移（`res.data.data.xxx` 语义不变）；token 走自定义 header `token`；`code ∈ [401,4011]` → `uni.$emit('session-expired')`；`code !== 200` → `uni.showToast` |
| 6 | 存储用 `uni.setStorageSync` 替代 js-cookie | 保留 `getToken/setToken/removeToken/getCookies/setCookies` 命名，减少迁移成本 |
| 7 | 设备类型固定 `PHONE(3)`，`deviceId` 用本地持久化随机串 | App 端不需要 UA 探测；`crypto.randomUUID` 在逻辑层不可靠 |
| 8 | UI 用 **wot-design-uni**（已装，easycom 已配 `^wd-(.*)`） | 替代 Element Plus |
| 9 | DOM 能力等价替换：`contenteditable` 富输入 → `textarea`；`v-html` → `rich-text`；`scrollTop` → `scroll-view`；`input[type=file]`/`FileReader` → `uni.chooseImage` + `uni.uploadFile` | App 逻辑层无 DOM |
| 10 | 直接删除：Tauri（`@tauri-apps/*`、`utils/tauri.ts`、窗口控件）、qiankun、vue-router、axios、Element Plus | mobile 无对应场景；路由改为 uni 页面栈 |

**注意**：SDK 的 `deviceFlag` 默认是 `Web(1)`，移动端初始化时应显式传 `DeviceFlag.App(0)`，与后端多端登录管控（PC/平板/手机分组互踢）保持一致。

---

## 接口契约（保持不变，后端 `customer-api/fg-chat`）

- 登录 `POST /chat/login`，body `{account, password, deviceType, deviceId}` → `{code, data:{token, imAddr:{wss_addr}}}` → 存 token + wss 地址
- 注册 `POST /auth/register`、验证码 `POST /send/code?email=`
- 用户信息 `POST /auth/userinfo`、`POST /update/info`、`POST /edit/password`、登出 `POST /auth/logout`
- 会话 `POST /conversation/list`（`{msgCount:1}`）
- 消息 `POST /message/list`（`{channelId, channelType, startMessageSeq, endMessageSeq:0, limit, pullMode:0}`）、`POST /message/send`（`{channelId,channelType,type,content,clientMsgNo?}`）
- 好友 `GET /friend/search|list`、`POST /friend/request/send|handle`、`GET /friend/request/received|sent`
- 群 `POST /group/create`
- 通知 `GET /notification/list|unread|unread/count`、`POST /notification/read/{id}`、`POST /notification/read/all`
- 上传 `POST /upload/image`、`POST /upload/avatar`，**multipart 字段名固定为 `file`**，服务端限制 **2MB**
- 用户 `GET /user/{uId}`、`GET /user/system/{systemKey}`

IM：`WKIM.init(url, {uid, token, deviceId, deviceFlag}, {singleton})` → `client.connect()` → `client.on(Connect/Disconnect/Message/Error)` → `client.send(channelId, channelType, payload)`；服务端转发与 IM 推送双通道并存。

消息类型常量沿用 `MessageType`：文本 1 / 图片 2 / 文件 3；系统与好友 100-103；RTC 200-210(**本期不做音视频**)；强制下线 220。

---

## 实施步骤

### Phase 1 · 基础设施（`chat-app/` 新增）

| 文件 | 内容 |
|---|---|
| `main.js` | 首行 `import './utils/polyfill'`；`createSSRApp` 后 `app.use(Pinia.createPinia())`，并 `return { app, Pinia }`（uni-app 要求返回 Pinia） |
| `utils/polyfill.js` | 缺失时才注入纯 JS `btoa/atob/TextEncoder/TextDecoder` 到 `globalThis` |
| `utils/config.js` | `BASE_API_URL`（App 无代理，用绝对地址，条件编译区分 dev/prod）、`WSS_KEY='xy_wss_key'`、`TOKEN_KEY='xy_chat_token'` |
| `utils/auth.js` | token / wss 地址读写（Storage 版） |
| `utils/device.js` | `getDeviceInfo()` → `{deviceType:3, deviceId}` |
| `utils/request.js` | `request/get/post/uploadFile`；统一 header、超时 15s、信封判定、401/4011 派发 `session-expired`、非 200 toast，**resolve 信封** |
| `libs/easyjssdk.js` | 复制自 pnpm store 的 ESM 单文件（顶部加注释标注版本与来源路径） |
| `utils/wk.js` | `decodePayload` / `extractMessageContent` / `decodeBase64FromJava`（去掉 TS 类型，保留三种 payload 形态兼容逻辑） |
| `App.vue` | `onLaunch`：有 token 则拉 userinfo + 初始化 IM；`onShow/onHide`：前后台切换时检查/恢复 IM 连接；监听 `uni.$emit('session-expired')` 与 IM 强下线 → 清登录态 + `uni.reLaunch('/pages/login/index')` |

### Phase 2 · API 层（`chat-app/api/`）

按上表契约新建 `auth.js`、`chat.js`、`friend.js`、`group.js`、`notification.js`、`upload.js`、`user.js`——函数签名与 y-chat-sub 的 `src/api/*` 一一对应，仅替换底层 request 与去掉 TS 类型；`rtc.js` 本期不建（音视频不在范围内）。

### Phase 3 · 状态与 IM（`chat-app/store/`、`services/im/`）

- `store/user.js`：`userInfo/currentChannel`、`userLogin`（带 deviceInfo）、`setUserInfo`、`userRegister`、`outLogin`（清 chat store → 清 token → reLaunch 登录页）
- `store/chat.js`：`contacts/activeContact/messages/conversations/connected`；`initWKIM`、`selectContact`、`loadHistoryMessages`（`scroll-view` 上拉分页）、`sendMessage`（乐观发送 + clientMsgNo 回填）、`addContact`、`resetChatData`
- `store/notification.js`：`notifications/unreadCount`；`fetchNotifications/fetchUnreadCount/markRead/markAllRead/registerIMListener/reset`
- `services/im/constants.js`、`types.js`：MessageType / ChannelType 等常量
- `services/im/wukong-im.js`：`WKIM.init(serverUrl, {uid, token, deviceId, deviceFlag:App}, {singleton:true})` 的单例封装，事件转发到 message-handler
- `services/im/message-handler.js`：按 type 分流（系统消息→通知 store + 全局提示；强下线→清登录态；普通消息→chat store）

### Phase 4 · 页面与路由（`chat-app/pages/`）

| 页面 | 对应 Tab / 入口 | 内容 |
|---|---|---|
| `pages/login/index.vue` | 启动页 | 登录 / 注册切换（`sendEmailCode` 120s 倒计时） |
| `pages/index/index.vue` | **首页（tab）** | 顶部导航（标题 + 通知铃铛带未读角标 + 头像入口）+ 会话列表 |
| `pages/chat/index.vue` | 非 tab（`navigateTo`，参数 `channelId/channelType/name`） | 消息 `scroll-view`（上拉加载历史）、消息气泡、底部输入栏（textarea + 选图 + 发送） |
| `pages/contact/index.vue` | **联系人（tab）** | 好友列表、搜索用户、好友请求（收到/发出） |
| `pages/community/index.vue` | **社区（tab）** | 占位页 |
| `pages/dynamic/index.vue` | **动态（tab）** | 占位页 |
| `pages/user/index.vue` | 首页头像入口（非 tab） | 个人信息、编辑资料/密码入口、退出登录 |
| `pages/notice/index.vue` | 首页铃铛（非 tab，可选） | 通知列表（复用 notification store） |

`pages.json` 调整：新增 `login`、`chat`、`user`（按需 `notice`）页面；启动页设为 `pages/login/index` 或由 `App.vue` 决定跳转；tabBar 保持四项 **文本型**（沿用现状，iconPath 可省略，若打包报错再补图标到 `static/`）。

### Phase 5 · 通用组件（`chat-app/components/`）

- `page-layout/page-layout.vue`：扩展为统一的「安全区 + 自定义导航 + slot」，供各 tab 页复用（现有文件为空）
- `conversation-item/`：会话/联系人条目（头像、昵称、最后消息、未读数）
- `message-bubble/`：文本 / 图片气泡、发送失败态、时间分割（替代 `MessageBox.vue`）
- `force-logout/`：会话过期 / 被踢下线 全局弹窗（替代 `ForceLogoutDialog` + `IncomingCallDialog` 中与本期无关的部分）
- `quick-menu/quick-menu.vue`：首页导航右侧的铃铛 + 头像入口

### Phase 6 · 联调与打包

`manifest.json` 补充网络/相机/相册权限与 iOS/Android 包名；真机跑通验收清单。

---

## 验证方式

无自动化测试，按以下清单在 **HBuilderX 运行到真机/模拟器** 逐项验收：

1. **登录链路**：登录成功 → token/wss 落 Storage → 拉 userinfo 成功 → IM 连接成功（`connected === true`）；错误密码有 toast
2. **IM 关键项**：`connect` 不报 `No base64 encoding method available`（验证 polyfill 生效）；心跳与断网重连正常；App 切后台再回前台连接可恢复
3. **消息**：会话列表加载正确（含未读数）；进聊天页拉到历史；发送文本/图片（先 `/upload/image` 再 `/message/send`）成功且乐观消息 clientMsgNo 正确回填；`scroll-view` 上拉分页
4. **联系人**：好友列表、搜索用户、发好友请求、处理收到的请求
5. **通知**：未读角标、通知列表、单条已读 / 全部已读
6. **异常流**：非 200 接口有 toast；`code=401/4011` 或 IM 强下线(220) → 弹窗提示并跳回登录页且本地登录态清空
7. **兼容性回归**：确认 y-chat-sub（网页端/桌面端）完全不受影响（本次只在 chat-app 内新增文件）

---

## 风险与兜底

| 风险 | 兜底方案 |
|---|---|
| App 逻辑层缺 `btoa/TextEncoder` 等，IM 发送报错 | Phase 1 的 polyfill 已覆盖；若仍失败，改为直接 patch `libs/easyjssdk.js` 的 Base64 分支改用 `uni.arrayBufferToBase64/uni.base64ToArrayBuffer` |
| `uni.connectSocket` 在 App 的 SocketTask 行为与小程序有差异（多连接限制、onMessage 数据类型） | 仅维持 **单一连接**；若返回二进制帧，polyfill 的 `TextDecoder` 兜底 |
| App 后台被挂起导致长连接断开/漏消息 | `App.vue onShow` 主动检测并重连；进入会话时以 `/message/list` 拉取补齐 |
| tabBar 无图标导致打包校验失败 | 补 8 张图标到 `static/` 并在 `pages.json` 配置 `iconPath/selectedIconPath` |
| 上传图片需先压缩（服务端 2MB 限制） | 用 `uni.compressImage` 或 `uni.chooseImage` 的 `sizeType:['compressed']` |
| 社区 / 动态为占位，可能产生误解 | 本期明确标注为占位，后续按需接入论坛模块 |

## 不在本期范围

WebRTC 音视频通话（200-210 信令 / CallOverlay / ringtone）、论坛与创作中心、Tauri 桌面端能力、多端 UI 自适应（仅面向移动端）。
