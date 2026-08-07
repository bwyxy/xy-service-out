# 好友与消息通知功能实现文档

## 1. 功能概述

本次实现围绕"好友关系"与"消息通知"两大业务域，新增以下能力：

- **好友搜索与添加**：支持按昵称、手机号、邮箱搜索用户，发送好友请求。
- **好友请求处理**：接收者可查看待处理请求并执行同意/拒绝操作。
- **好友关系管理**：同意请求后自动建立双向好友关系，并提供好友列表查询。
- **消息通知系统**：好友请求、同意/拒绝等事件生成通知记录，并通过 WukongIM 推送实时消息。
- **通知中心**：前端提供通知列表、未读数量、单条/全部已读能力。
- **并发控制**：针对发送请求、处理请求等关键操作使用 Redis 分布式锁，防止重复与冲突。

## 2. 数据模型

### 2.1 t_friend（好友关系表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| u_id | bigint | 用户 ID |
| friend_id | bigint | 好友用户 ID |
| status | char(1) | 0-正常，1-拉黑 |
| create_time | datetime | 添加时间 |
| update_time | datetime | 更新时间 |

约束：`unique index uk_user_friend (u_id, friend_id)`，避免重复好友关系。

### 2.2 t_friend_request（好友请求表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| from_uid | bigint | 发送者 ID |
| to_uid | bigint | 接收者 ID |
| status | tinyint | 0-待处理 1-已同意 2-已拒绝 3-已取消 4-已过期 |
| apply_msg | varchar(255) | 申请备注 |
| reply_msg | varchar(255) | 处理回复 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

约束：`unique index uk_from_to (from_uid, to_uid)`，避免同一方向重复请求；`index idx_to_uid_status (to_uid, status)` 加速收件箱查询。

### 2.3 t_notification（通知表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| u_id | bigint | 接收用户 ID |
| type | tinyint | 1-好友请求 2-好友同意 3-系统通知 |
| title | varchar(100) | 标题 |
| content | varchar(500) | 内容 |
| related_id | bigint | 关联业务 ID |
| is_read | tinyint | 0-未读 1-已读 |
| extra | json | 扩展字段 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

索引：`idx_user_read (u_id, is_read)`、`idx_user_type (u_id, type)`。

## 3. 后端实现

### 3.1 好友服务（FriendServiceImpl）

位置：`chat-api/fg-chat/src/main/java/com/chat/fg/service/impl/FriendServiceImpl.java`

核心方法：

- `searchUsers(Long currentUid, String keyword)`：排除当前用户，按昵称/手机号/邮箱模糊匹配，最多返回 20 条。
- `sendFriendRequest(Long fromUid, FriendRequestDTO dto)`：
  - 校验：不能添加自己、目标用户存在且未禁用、非好友。
  - 分布式锁：`friend:request:{minUid}:{maxUid}`，防止双向并发请求。
  - 重复检查：若已有待处理请求则拒绝；若对方已向我发送请求则自动同意。
  - 落库并给接收者发送通知。
- `handleFriendRequest(Long toUid, HandleFriendRequestDTO dto)`：
  - 校验：请求存在、当前用户有权处理、状态为待处理。
  - 分布式锁：`friend:handle:{requestId}`，防止同一请求被并发处理。
  - 同意时插入双向好友关系并通知发送者；拒绝时通知发送者。
- `listFriends / listReceivedRequests / listSentRequests`：列表查询，组装用户昵称/头像信息。

### 3.2 通知服务（NotificationServiceImpl）

位置：`chat-api/fg-chat/src/main/java/com/chat/fg/service/impl/NotificationServiceImpl.java`

核心方法：

- `notifyUser(Long uId, NotificationType type, String title, String content, Long relatedId, String extra)`：持久化通知并调用 `WKClientService.sendSystemMessage` 推送。
- `listUnread / listNotifications / markRead / markAllRead / countUnread`：未读与列表管理。

### 3.3 分布式锁（DistributedLockHelper）

位置：`chat-api/fg-chat/src/main/java/com/chat/fg/utils/DistributedLockHelper.java`

基于 Redis `SET NX EX` 实现自旋等待锁，提供 `Supplier` 与 `Runnable` 两种重载。获取失败时抛出 `ConcurrentOperationException`，携带清晰业务提示：

- 发送请求冲突："操作过于频繁，请稍后重试"
- 处理请求冲突："请求正在处理中，请稍后重试"

### 3.4 控制层接口

#### FriendController

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/friend/search` | 搜索用户 |
| POST | `/friend/request/send` | 发送好友请求 |
| POST | `/friend/request/handle` | 处理好友请求（1-同意 2-拒绝） |
| GET | `/friend/request/received` | 收到的好友请求 |
| GET | `/friend/request/sent` | 发送的好友请求 |
| GET | `/friend/list` | 我的好友列表 |

#### NotificationController

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/notification/list` | 通知列表（支持 type/isRead 过滤分页） |
| GET | `/notification/unread` | 未读通知列表 |
| GET | `/notification/unread/count` | 未读数量 |
| POST | `/notification/read/{notificationId}` | 单条已读 |
| POST | `/notification/read/all` | 全部已读 |

### 3.5 序列化配置

位置：`chat-api/fg-chat/src/main/java/com/chat/fg/config/JacksonConfig.java`

注册 `JavaTimeModule` 并禁用 `WRITE_DATES_AS_TIMESTAMPS`，确保 `LocalDateTime` 以可读字符串返回。

## 4. 前端实现

### 4.1 添加好友弹窗

位置：`y-micro-app/apps/y-chat-sub/src/views/chat/components/SearchFun/SearchUserDialog.vue`

- 支持输入昵称/手机号/邮箱进行防抖搜索。
- 展示用户列表（头像、昵称、联系方式）。
- 可填写附加消息后点击"添加"，调用 `POST /friend/request/send`。

### 4.2 通知中心

位置：`y-micro-app/apps/y-chat-sub/src/layout/Header/NotificationCenter.vue`

- 弹窗形式展示通知列表，区分已读/未读（红点、标签）。
- 支持点击单条标记已读、一键全部已读。
- 调用 `/notification/list`、`/notification/read/{id}`、`/notification/read/all`。

### 4.3 API 封装

- `y-micro-app/apps/y-chat-sub/src/api/friend.ts`：好友相关接口。
- `y-micro-app/apps/y-chat-sub/src/api/notification.ts`：通知相关接口。

## 5. 并发控制说明

### 5.1 场景

1. **重复发送好友请求**：同一用户连续快速点击"添加"。
2. **双方同时发送请求**：A 向 B 发送请求的同时 B 也向 A 发送。
3. **同时处理同一请求**：两个终端同时点击"同意"。

### 5.2 策略

- 使用 Redis 分布式锁对关键资源加锁。
- 锁 Key 设计：
  - 发送请求：`friend:request:{minUid}:{maxUid}`（按用户 ID 排序，保证同一对用户使用同一把锁）。
  - 处理请求：`friend:handle:{requestId}`（按请求 ID 加锁）。
- 锁内执行二次校验，确保状态一致。
- 冲突时返回明确中文提示，避免歧义。

### 5.3 数据一致性兜底

- 数据库唯一索引 `uk_from_to` 阻止重复请求。
- `createFriendRelation` 方法内再次查询，已存在则跳过插入，防止锁外并发产生脏数据。

## 6. 关键文件清单

| 文件 | 说明 |
|------|------|
| `chat-api/fg-chat/src/main/java/com/chat/fg/service/impl/FriendServiceImpl.java` | 好友业务核心实现 |
| `chat-api/fg-chat/src/main/java/com/chat/fg/controller/FriendController.java` | 好友接口 |
| `chat-api/fg-chat/src/main/java/com/chat/fg/service/impl/NotificationServiceImpl.java` | 通知业务实现 |
| `chat-api/fg-chat/src/main/java/com/chat/fg/controller/NotificationController.java` | 通知接口 |
| `chat-api/fg-chat/src/main/java/com/chat/fg/utils/DistributedLockHelper.java` | Redis 分布式锁 |
| `chat-api/fg-chat/src/main/java/com/chat/fg/utils/ConcurrentOperationException.java` | 并发冲突异常 |
| `chat-api/fg-chat/src/main/java/com/chat/fg/config/JacksonConfig.java` | 时间序列化配置 |
| `chat-api/fg-chat/src/test/java/com/chat/fg/service/FriendServiceImplTest.java` | 好友服务单元测试 |
| `chat-api/fg-chat/src/test/java/com/chat/fg/controller/FriendControllerIntegrationTest.java` | 好友控制器集成测试 |
| `y-micro-app/apps/y-chat-sub/src/views/chat/components/SearchFun/SearchUserDialog.vue` | 添加好友前端 |
| `y-micro-app/apps/y-chat-sub/src/layout/Header/NotificationCenter.vue` | 通知中心前端 |
| `sql/chat.sql` | 数据库表结构 |

## 7. 注意事项

- 集成测试使用 H2 内存数据库与 `application-test.yml`，并将 `WKClientService` 与 `DistributedLockHelper` 通过 `@MockBean` 替换，避免依赖外部 Redis 与 IM 服务。
- 通知服务中的 IM 推送失败不影响主业务流程，仅记录错误日志。
- 好友搜索最多返回 20 条，避免大数据量查询。
