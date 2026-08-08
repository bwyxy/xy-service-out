# 前端悟空 IM 连接代码优化总结

## 1. 优化目标

对 `y-chat-sub` 微应用中连接悟空 IM（WuKongIM）的前端代码进行全面的代码风格与结构优化，提升可读性、可维护性与可扩展性，同时保持 IM 连接、消息收发等核心功能不变。

## 2. 优化前问题梳理

| 问题 | 说明 |
|------|------|
| 连接逻辑分散 | IM 初始化、事件监听直接写在 `store/user.ts` 中，与用户信息/认证逻辑耦合。 |
| 缺失模块化封装 | 没有统一的 IM 服务层，`store/chat.ts` 引用了 `utils/wk.ts` 中不存在的 `initWK`、`getIM`、`sendPersonalMessage` 等函数。 |
| 类型缺失 | 大量使用 `any`，如事件回调、消息对象、用户信息等均无类型。 |
| 代码风格不统一 | 参数类型注解前空格不一致（如 `params :any`）、引号混用、缩进不规范。 |
| 空文件残留 | `utils/wkContent.ts` 为空且未被引用。 |
| 连接逻辑隐患 | `user.ts` 中 `connect().then(...).then(...)` 链式调用语义错误，未真正发送消息。 |

## 3. 优化内容

### 3.1 新增 IM 服务模块（services/im）

新增目录 `y-micro-app/apps/y-chat-sub/src/services/im/`，将 IM 连接能力集中管理：

- `types.ts`：定义 `WukongIMConfig`、`WukongIMMessage`、`WukongIMConversation`、`WukongIMEventHandler` 等类型。
- `wukong-im.ts`：封装 `WukongIMService` 单例服务，提供：
  - `connect(config)`：基于 easyjssdk 初始化并连接。
  - `disconnect()`：断开连接并清理资源。
  - `onMessage / onConversationUpdate / onConnect / onDisconnect / onError`：事件订阅。
  - `sendPersonalMessage(channelId, payload)` / `sendGroupMessage(channelId, payload)`：发送单聊/群聊消息。
  - `syncMessage(...)`：预留接口，明确标注需配合后端 REST API 使用。
- `index.ts`：统一导出。
- `constants.ts`：新增 `MessageType` 消息类型枚举与系统消息判断函数。
- `message-handler.ts`：新增 `MessageHandlerService`，按消息类型分发：
  - 普通聊天消息（TEXT/IMAGE/FILE）分发给 `chat store`。
  - 系统/业务通知（SYSTEM、FRIEND_REQUEST、FRIEND_ACCEPTED 等）通过 `ElNotification` 弹窗提醒，并同步给 `notification store` 增加未读数。

### 3.2 新增通知 Store（store/notification.ts）

- 集中管理通知列表、未读数量、加载状态。
- `fetchUnreadCount / fetchNotifications / markRead / markAllRead`：与后端通知 API 交互。
- `registerIMListener()`：注册 IM 系统消息监听，收到后实时增加未读数并追加本地通知记录。
- `reset()`：退出登录时清空状态。

### 3.3 重构工具函数（utils/wk.ts）

- 保留 `decodePayload` 与 `decodeBase64FromJava`。
- 引入 `WukongIMMessagePayload` 类型，避免 `any`。
- 补充 JSDoc 注释，统一错误日志前缀 `[WukongIM]`。
- 删除空文件 `utils/wkContent.ts`。

### 3.4 重构用户 Store（store/user.ts）

- 移除 `wkIM` 状态字段，IM 实例由 `WukongIMService` 单例持有。
- 为 `UserInfo`、`CurrentChannel` 增加类型定义并导出。
- `initWKIM()` 改为从 cookie 读取 wss 地址、从 `userInfo` 读取 uid，调用 `wukongIMService.connect()`，并初始化 `MessageHandlerService`、注册通知监听。
- `outLogin()` 中调用 `wukongIMService.disconnect()` 与 `notificationStore.reset()`，确保退出时释放连接并清空通知状态。
- 统一类型注解、引号、缩进风格。

### 3.5 重构聊天 Store（store/chat.ts）

- 移除对 `@/utils/wk` 中不存在函数的引用。
- 使用 `wukongIMService` 完成连接，使用 `messageHandlerService.onChatMessage` 处理聊天消息。
- 收到消息时自动更新联系人 `lastMessage`、`lastMessageTime`、`unreadCount`。
- 修正 `login` API 调用方式（由两个参数改为对象参数）。
- 为 `Contact`、`Message` 等定义类型，补充函数注释。
- `sendMessage` 改为 `async/await`，增加空值校验。

### 3.6 重构布局与组件

- `layout/index.vue`：使用 `wukongIMService.isInitialized()` 替代对 `wkIM` store 状态的判断，保持初始化逻辑清晰。
- `views/chat/components/ContactList/index.vue`：补充 `ConversationItem` 等类型，统一导入顺序与缩进，增加 `v-loading`，规范事件命名。
- `views/chat/components/ChatWindow/ChatWindow.vue`：增加 `currentChannel` 空值保护，消除 TypeScript 报错。
- `views/chat/components/SearchFun/FriendRequestDialog.vue`：移除未使用的 `activeTab`。

## 4. 测试验证

### 4.1 类型检查与构建

执行命令：

```bash
cd y-micro-app/apps/y-chat-sub
pnpm run build
```

结果：

- `vue-tsc -b` 类型检查通过，无类型错误。
- `vite build` 构建成功，输出到 `dist/` 目录。
- 构建产物中包含 `easyjssdk` 相关 chunk，说明 IM SDK 已正确打包。

### 4.2 功能验证说明

由于完整的 IM 连接功能依赖后端服务（登录接口返回 token 与 wss 地址）及悟空 IM 服务端，本次在本地未启动完整后端链路的情况下，通过以下方式验证：

1. **编译时静态检查**：`vue-tsc` 覆盖所有 `.ts` 与 `.vue` 文件，确认 IM 相关 API 调用、事件订阅、类型解构均无错误。
2. **构建产物检查**：确认 easyjssdk 被正确打包，连接、发送、事件监听等代码路径均存在于产物中。
3. **调用链路检查**：
   - 登录成功后 `userStore.userLogin` 设置 token 与 wss cookie。
   - 路由守卫 `setUserInfo()` 获取用户信息。
   - 布局组件 `layout/index.vue` 在 `userInfo.uid` 存在时触发 `initWKIM()`。
   - `initWKIM()` 调用 `wukongIMService.connect()` 建立连接，并初始化 `MessageHandlerService`。
   - 聊天消息经 `MessageHandlerService` 分发到 `chat store`。
   - `chat store` 的 `syncConversationFromMessage` 会自动更新/新增联系人、刷新最后一条消息、增加未读数，并按最后消息时间倒序排序，实现最新消息置顶。
   - 系统/业务通知经 `MessageHandlerService` 触发 `ElNotification` 弹窗，并同步到 `notification store` 增加未读数。
   - `Header` 与 `NotificationCenter` 通过 `notification store` 共享未读数与通知列表。
   - `ContactList` 组件以 `chat store.contacts` 为会话列表数据源，支持未读角标、激活态、最后消息时间展示。

建议在完整后端环境就绪后，进行以下运行时验证：

- 登录后浏览器控制台输出 `[WukongIM] 连接成功`。
- 向好友发送文本消息后，对端能实时收到消息；左侧会话列表对应联系人置顶，并显示未读角标与最后一条消息。
- 收到好友请求/同意等系统通知时，右上角弹出 `ElNotification` 提示，Header 消息通知 badge 数字增加。
- 切换不同联系人时，未读角标清零，消息列表切换。
- 退出登录后 IM 连接断开，通知未读数清零。

## 5. 关键文件清单

| 文件 | 变更说明 |
|------|----------|
| `y-micro-app/apps/y-chat-sub/src/services/im/types.ts` | 新增 IM 类型定义 |
| `y-micro-app/apps/y-chat-sub/src/services/im/wukong-im.ts` | 新增 IM 服务核心实现 |
| `y-micro-app/apps/y-chat-sub/src/services/im/index.ts` | 新增模块导出 |
| `y-micro-app/apps/y-chat-sub/src/services/im/constants.ts` | 新增消息类型常量 |
| `y-micro-app/apps/y-chat-sub/src/services/im/message-handler.ts` | 新增消息分类处理服务 |
| `y-micro-app/apps/y-chat-sub/src/utils/wk.ts` | 重构 payload 解码工具 |
| `y-micro-app/apps/y-chat-sub/src/store/user.ts` | 解耦 IM 逻辑，增加类型 |
| `y-micro-app/apps/y-chat-sub/src/store/chat.ts` | 使用新 IM 服务，处理聊天消息与未读 |
| `y-micro-app/apps/y-chat-sub/src/store/notification.ts` | 新增通知状态管理 |
| `y-micro-app/apps/y-chat-sub/src/layout/index.vue` | 更新 IM 初始化判断 |
| `y-micro-app/apps/y-chat-sub/src/layout/Header/index.vue` | 使用 notification store，每 60 秒同步未读数 |
| `y-micro-app/apps/y-chat-sub/src/layout/Header/NotificationCenter.vue` | 使用 notification store 加载/标记通知 |
| `y-micro-app/apps/y-chat-sub/src/views/chat/components/ContactList/index.vue` | 增加类型、优化样式 |
| `y-micro-app/apps/y-chat-sub/src/views/chat/components/ChatWindow/ChatWindow.vue` | 增加空值保护 |
| `y-micro-app/apps/y-chat-sub/src/views/chat/components/SearchFun/FriendRequestDialog.vue` | 移除未使用变量 |
| `y-micro-app/apps/y-chat-sub/src/utils/wkContent.ts` | 删除空文件 |

## 6. 后续建议

1. 补充 `WukongIMService` 的单元测试，模拟 `easyjssdk` 的 `WKIM.init`、`on`、`connect`、`send` 方法。
2. 待后端消息同步接口稳定后，完善 `syncMessage` 的实现（可直接调用 REST API 而非 SDK）。
3. 将 IM 连接状态（connected/disconnected）暴露为响应式状态，供 UI 展示连接中/已断开提示。
4. 接入项目 ESLint/Prettier（如已配置），进一步统一代码风格。
