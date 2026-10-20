# xy-service

## 1. 项目概述

本项目是一个基于 **Spring Boot 3** 后端与 **Vue 3 + qiankun 微前端** 架构的综合性服务平台，集成 **悟空 IM（WukongIM）** 提供实时聊天能力。系统采用多模块 Maven 与 Monorepo 前端组织方式，支持业务模块独立开发、独立部署。

### 1.1 主要业务

- **用户中心**：注册、登录、用户信息管理、头像上传。
- **即时通讯（飞鸽聊天）**：单聊、群聊、好友关系、会话列表、历史消息、富文本输入（文本/表情/图片）。
- **音视频通话**：基于自建 LiveKit SFU 的单聊/群聊通话（单房间最多 9 人），呼叫控制信令复用悟空 IM。
- **消息通知**：好友请求、好友同意、系统通知的实时推送与通知中心。
- **内容管理（博客/CMS）**：文章、分类、标签、评论、菜单、配置。
- **任务协作（Todo）**：任务、标签、协作者、微信登录。
- **AI 助手（xy-ai）**：多模型对话（同步/流式）、RAG 知识库检索、Agent 工具调用、用户配额管理、API Key 认证。
- **管理后台**：用户、权限、菜单、内容审核、AI API Key 管理等后台管理能力。

### 1.2 项目定位

- 后端按业务领域垂直拆分，便于团队并行开发与独立演进。
- 前端采用 qiankun 微前端，主应用作为基座，子应用可独立运行与发布。
- 聊天模块通过后端 HTTP 接口转发消息，便于后续接入内容安全、敏感词、图片审核等能力。

***

## 2. 系统架构

### 2.1 后端架构

```
xy-service (parent)
├── xy-common          # 公共实体、工具类、枚举、Feign Client、异常定义
├── xy-framework       # 安全、JWT、全局异常、限流、Web 配置
├── xy-system          # 系统服务：用户、权限、菜单、文章、博客、AI API Key、七牛云、悟空 IM Client
├── xy-admin           # 管理后台 REST API
├── common-api         # 公共 API 模块父工程
│   └── xy-ai          # AI 能力层：多模型对话、RAG、Agent、API Key 换 token（端口 9070）
├── chat-api           # 聊天模块父工程
│   └── fg-chat        # 飞鸽聊天主 API（端口 9080）
└── customer-api       # 客户/用户端 API
    ├── xy-app         # Todo 任务、标签、协作者、微信登录（端口 9100）
    └── xy-blog        # 博客前台 API
```

### 2.2 前端架构

```
y-micro-app (pnpm workspace)
├── apps/
│   ├── main           # qiankun 主应用（基座，端口 5000）
│   ├── y-chat-sub     # 聊天子应用（端口 9001）
│   └── y-admin-sub    # 管理后台子应用（端口 9002）
└── packages/
    ├── xy-utils       # 公共工具库
    └── xy-qiankun-communication  # 微前端通信库
```

### 2.3 即时通讯架构

```
┌─────────────┐     HTTP      ┌──────────────┐     HTTP      ┌─────────────┐
│  前端子应用  │  ──────────▶  │  fg-chat     │  ──────────▶  │  悟空 IM    │
│ y-chat-sub  │   WebSocket   │  (端口 9080)  │   Feign      │  服务端     │
└─────────────┘  ◀──────────  └──────────────┘  ◀──────────  └─────────────┘
```

- 前端通过 `easyjssdk` 与悟空 IM 建立 WebSocket 长连接，接收实时消息。
- 发送消息时统一调用后端 `/message/send` HTTP 接口，由后端转发至悟空 IM，便于后续做内容校验。
- 会话列表与历史消息通过后端 `/conversation/list`、`/message/list` 同步并补充本地用户/群组元数据。

### 2.4 音视频通话架构

```
┌─────────────┐  200-205 呼叫信令（悟空 IM 个人频道）  ┌──────────────┐
│  前端子应用  │ ────────────────────────────────▶  │  fg-chat     │
│ y-chat-sub  │   /rtc/* 房间管理、Token 签发          │  (端口 9080)  │
└──────┬──────┘ ◀────────────────────────────────  └──────┬───────┘
       │ wss 接入信令 + WebRTC 媒体流                       │ RoomService / AccessToken
       ▼                                                  ▼
┌──────────────────────────────────────────────────────────────────┐
│                    LiveKit SFU（自建，媒体转发）                    │
└──────────────────────────────────────────────────────────────────┘
```

- 呼叫控制（邀请/接听/拒绝/挂断/取消/结束）复用悟空 IM 个人频道的 `200-205` 消息类型传输，不新增长连接。
- 媒体面由自建 LiveKit SFU 转发，单聊与群聊共用同一套媒体层，单房间人数上限 9。
- 接入凭证由后端签发 LiveKit `AccessToken`（含房间与发布/订阅授权），前端使用 `livekit-client` 加入房间。
- 房间与参与者状态落 `t_rtc_room` / `t_rtc_participant`，定时任务负责超时呼叫、超长通话与异常残留房间的兜底回收。
- 部署要求见 [音视频通话部署文档](./docs/rtc/deployment.md)，架构说明见 [音视频通话设计文档](./docs/rtc/design.md)。

### 2.5 AI 模块架构

```
┌─────────────────────────────────────────────────────────────┐
│                         Controller 层                        │
│   /ai/chat  /ai/chat/stream  /ai/conversation  /ai/provider  │
│   /ai/knowledge  /ai/auth/token                              │
├─────────────────────────────────────────────────────────────┤
│                         Service 层                           │
│   AiChatService  AiConversationService  AiProviderService    │
│   AiKnowledgeBaseService  AiQuotaService                     │
├─────────────────────────────────────────────────────────────┤
│                         Provider 层                          │
│   ModelProvider (接口)                                       │
│   ├─ OpenAiModelProvider        OpenAI 兼容接口              │
│   └─ DashscopeModelProvider     阿里云百炼                   │
├─────────────────────────────────────────────────────────────┤
│                         RAG 层                               │
│   DocumentChunker  EmbeddingService  VectorStoreService      │
├─────────────────────────────────────────────────────────────┤
│                         Agent 层                             │
│   ToolRegistry  ToolExecutor  ToolDefinition                 │
├─────────────────────────────────────────────────────────────┤
│                         数据层                               │
│   MySQL：ai_model_provider / ai_conversation / ai_message    │
│         ai_knowledge_base / ai_knowledge_document            │
│         ai_knowledge_chunk / ai_tool / ai_user_quota         │
└─────────────────────────────────────────────────────────────┘
```

- `xy-ai` 作为统一 AI 能力层独立部署，向上层业务提供同步/流式对话、RAG 检索、Agent 工具调用与配额控制。
- 模型提供者通过 `ModelProvider` 接口抽象，新增模型无需改动上层业务代码。
- API Key 认证核心逻辑与数据层下沉至 `xy-system`，`xy-ai` 仅保留 token 换取能力，`xy-admin` 负责密钥生命周期管理。

***

## 3. 核心功能模块

### 3.1 认证与授权

- 基于 Spring Security 6 + JWT 的无状态认证。
- 支持聊天用户（`chat_{uid}`）与管理后台用户双数据源登录。
- 匿名白名单通过 `@Anonymous` 注解与 `PermitAllUrlProperties` 配置。

### 3.2 用户中心

- 用户注册：邮箱验证码、BCrypt 密码加密。
- 用户信息查询与修改：昵称、头像、性别、密码。
- 头像上传：后端对接七牛云对象存储。

### 3.3 即时通讯

| 功能    | 说明                                    |
| ----- | ------------------------------------- |
| 单聊/群聊 | 基于悟空 IM 频道（channel）实现                 |
| 好友关系  | 搜索用户、发送请求、同意/拒绝、双向好友关系                |
| 会话列表  | 同步悟空 IM 会话，补充昵称/头像，支持未读角标             |
| 历史消息  | 选择联系人后从后端拉取历史消息                       |
| 富文本输入 | 支持文本、Emoji、图片（拖拽/粘贴/上传），图片自动压缩        |
| 消息去重  | 基于 `clientMsgNo` / `messageId` 防止重复渲染 |

### 3.4 消息通知

- 好友请求、好友同意、系统通知生成通知记录。
- 通过悟空 IM 系统消息实时推送到接收方。
- 前端通知中心展示未读数量、通知列表，支持单条/全部已读。

### 3.5 AI 模块

| 功能         | 说明                                                          |
| ---------- | ----------------------------------------------------------- |
| 多模型对话      | 支持同步 `/ai/chat` 与流式 `/ai/chat/stream` 两种对话方式                |
| 模型提供者      | OpenAI 兼容接口、阿里云百炼（DashScope），通过 `ai_model_provider` 表动态配置   |
| RAG 知识库    | 文档分块、向量检索、上下文增强，支持文本与 PDF 文档                                |
| Agent 工具   | 动态加载 `ai_tool` 表中的工具定义，支持 `FunctionToolCallback` 调用         |
| 用户配额       | `AiQuotaService` 按用户维度控制每日调用次数                              |
| API Key 认证 | `POST /ai/auth/token` 使用 API Key + Secret 换取 JWT，支持启用/禁用/删除 |

### 3.6 并发控制

- 针对发送好友请求、处理好友请求等关键操作使用 Redis 分布式锁。
- 数据库唯一索引兜底，防止重复请求与重复好友关系。

### 3.7 音视频通话

| 功能   | 说明                                                          |
| ---- | ----------------------------------------------------------- |
| 单聊通话 | 1v1 语音/视频，被叫振铃，呼叫超时（默认 90 秒）自动取消并通知双方                       |
| 群聊通话 | 通过群成员选择器邀请入会，单房间最多 9 人，宫格布局展示                               |
| 通话浮层 | 支持最小化悬浮窗、静音、开关摄像头、挂断，通话中可查看成员状态                             |
| 通话记录 | 通话结束写入聊天记录（群聊记录带「N 人」标识），可在会话内回看                            |
| 状态兜底 | 定时任务回收超时呼叫、超长通话（默认 180 分钟）与异常残留房间；前端异常退出时也会补发挂断上报，避免房间卡在通话中 |

***

## 4. 技术栈

### 4.1 后端

| 类别     | 技术                                       | 版本            |
| ------ | ---------------------------------------- | ------------- |
| 基础框架   | Spring Boot                              | 3.2.4         |
| 安全框架   | Spring Security                          | 6.x           |
| 服务间调用  | Spring Cloud OpenFeign                   | 2023.0.3      |
| ORM 框架 | MyBatis-Plus                             | 3.5.5 / 3.5.9 |
| 数据库    | MySQL                                    | 8.0.33        |
| 连接池    | Druid                                    | 1.2.27        |
| 缓存     | Redis                                    | -             |
| 限流     | Bucket4j                                 | 8.0.1         |
| 文档     | Swagger / Knife4j                        | -             |
| 即时通讯   | 悟空 IM（WukongIM）                          | -             |
| 音视频媒体  | LiveKit Server（自建 SFU）                    | 1.13.x        |
| AI 框架  | Spring AI                                | 1.1.0         |
| 向量检索   | SimpleVectorStore（可扩展 Milvus / pgvector） | -             |
| 构建工具   | Maven                                    | -             |
| JDK    | Java                                     | 17            |

### 4.2 前端

| 类别      | 技术                            | 说明              |
| ------- | ----------------------------- | --------------- |
| 框架      | Vue 3                         | Composition API |
| 语言      | TypeScript                    | -               |
| 构建工具    | Vite 7                        | -               |
| 微前端     | qiankun + vite-plugin-qiankun | -               |
| 状态管理    | Pinia                         | -               |
| 路由      | Vue Router 5                  | -               |
| UI 组件库  | Element Plus                  | -               |
| CSS 预处理 | Sass / SCSS                   | -               |
| IM SDK  | easyjssdk                     | 悟空 IM 官方 SDK    |
| 音视频 SDK | livekit-client                | WebRTC 客户端       |
| 包管理     | pnpm workspace                | catalog 统一版本    |

***

## 5. 环境要求

### 5.1 开发环境

- **JDK**：17+
- **Node.js**：18+
- **pnpm**：8+
- **Maven**：3.9+
- **MySQL**：8.0+
- **Redis**：6.0+
- **悟空 IM**：需部署或接入外部悟空 IM 服务

### 5.2 数据库

初始化脚本位于 `sql/` 目录：

- `ai.sql`：AI 模块表结构（模型提供者、会话、消息、知识库、文档、工具、配额、API Key 等）
- `chat.sql`：聊天模块表结构（用户、好友、好友请求、通知、群组等）
- `blog.sql`：博客/CMS 模块表结构
- `todo.sql`：Todo 任务模块表结构
- `comic.sql`：其他业务表结构

### 5.3 外部依赖

- **七牛云**：用于图片/头像上传，需在配置中填写 `AccessKey`、`SecretKey`、`Bucket`、`Domain`。
- **悟空 IM**：用于实时消息收发，需配置 `appId`、`appKey`、`apiUrl`、`wsUrl`。
- **LiveKit SFU**：用于音视频媒体转发，需自建部署并配置 `LIVEKIT_URL`、`LIVEKIT_API_URL`、`LIVEKIT_API_KEY`、`LIVEKIT_API_SECRET`；`api-key`/`api-secret` 必须与 LiveKit 服务端 `keys` 一致，部署与端口放行要求见 [音视频通话部署文档](./docs/rtc/deployment.md)。
- **邮箱服务**：用于发送注册验证码，需配置 SMTP 信息。
- **AI 模型服务**：需在 `ai_model_provider` 表中配置 endpoint、api\_key、default\_model 等，支持 OpenAI 兼容接口与阿里云百炼。

***

## 6. 快速开始

### 6.1 后端启动

1. 创建数据库并执行 `sql/` 目录下的初始化脚本。
2. 复制 `.env.example` 为 `.env`，填写数据库、Redis、七牛云、悟空 IM、邮箱等配置。
3. 编译项目：

```bash
mvn clean compile -DskipTests
```

1. 启动 `fg-chat`：

```bash
mvn spring-boot:run -pl chat-api/fg-chat -am
```

1. 启动 `xy-ai`（AI 能力服务，端口 9070）：

```bash
mvn spring-boot:run -pl common-api/xy-ai -am
```

1. 启动其他业务模块（按需）：

```bash
mvn spring-boot:run -pl xy-admin -am
mvn spring-boot:run -pl customer-api/xy-app -am
mvn spring-boot:run -pl customer-api/xy-blog -am
```

### 6.2 前端启动

1. 安装依赖：

```bash
cd y-micro-app
pnpm install
```

1. 启动主应用：

```bash
cd apps/main
pnpm run dev
```

1. 启动聊天子应用：

```bash
cd apps/y-chat-sub
pnpm run dev
```

1. 启动管理后台子应用（按需）：

```bash
cd apps/y-admin-sub
pnpm run dev
```

### 6.3 构建

后端：

```bash
mvn clean package -DskipTests
```

前端（以聊天子应用为例）：

```bash
cd y-micro-app/apps/y-chat-sub
pnpm run build
```

***

## 7. 数据库设计

### 7.1 聊天模块核心表

| 表名                 | 说明    |
| ------------------ | ----- |
| `t_user`           | 聊天用户表 |
| `t_friend`         | 好友关系表 |
| `t_friend_request` | 好友请求表 |
| `t_notification`   | 通知表   |
| `t_group`          | 群组表   |
| `t_group_member`   | 群组成员表 |
| `t_rtc_room`       | 音视频通话房间表 |
| `t_rtc_participant` | 音视频通话参与者表 |

### 7.2 关键约束

- `t_friend`：`unique index uk_user_friend (u_id, friend_id)`
- `t_friend_request`：`unique index uk_from_to (from_uid, to_uid)`
- `t_notification`：`index idx_user_read (u_id, is_read)`
- `t_rtc_participant`：`unique index uk_room_user (room_id, u_id)`

***

## 8. API 接口概览

### 8.1 聊天模块（fg-chat，端口 9080）

| 接口                           | 方法   | 说明       |
| ---------------------------- | ---- | -------- |
| `/auth/register`             | POST | 用户注册     |
| `/chat/login`                | POST | 用户登录     |
| `/auth/userinfo`             | POST | 获取当前用户信息 |
| `/send/code`                 | POST | 发送邮箱验证码  |
| `/user/{uId}`                | GET  | 查询用户信息   |
| `/friend/search`             | GET  | 搜索用户     |
| `/friend/request/send`       | POST | 发送好友请求   |
| `/friend/request/handle`     | POST | 处理好友请求   |
| `/friend/list`               | GET  | 好友列表     |
| `/notification/list`         | GET  | 通知列表     |
| `/notification/unread/count` | GET  | 未读通知数量   |
| `/notification/read/{id}`    | POST | 单条已读     |
| `/conversation/list`         | POST | 会话列表     |
| `/message/list`              | POST | 历史消息     |
| `/message/send`              | POST | 发送消息     |
| `/upload/image`              | POST | 上传图片     |
| `/rtc/call`                  | POST | 发起通话（单聊/群聊） |
| `/rtc/accept`                | POST | 接听通话     |
| `/rtc/reject`                | POST | 拒绝通话     |
| `/rtc/cancel`                | POST | 取消呼叫     |
| `/rtc/hangup`                | POST | 挂断通话     |
| `/rtc/invite`                | POST | 群聊邀请成员入会 |
| `/rtc/end`                   | POST | 主持人结束房间  |
| `/rtc/token`                 | POST | 获取 LiveKit 接入 Token |
| `/rtc/room/{roomId}`         | GET  | 查询房间详情   |
| `/rtc/health`                | GET  | LiveKit 健康检查 |

### 8.2 AI 模块（xy-ai，端口 9070）

| 接口                       | 方法                  | 说明                      |
| ------------------------ | ------------------- | ----------------------- |
| `/ai/chat`               | POST                | 同步对话                    |
| `/ai/chat/stream`        | POST                | 流式对话（SSE）               |
| `/ai/conversation`       | GET/POST            | 会话列表 / 创建会话             |
| `/ai/provider`           | GET/POST/PUT/DELETE | 模型提供者配置管理               |
| `/ai/knowledge/base`     | GET/POST/PUT/DELETE | 知识库管理                   |
| `/ai/knowledge/document` | POST/DELETE         | 知识库文档管理                 |
| `/ai/knowledge/retrieve` | GET                 | 知识库检索                   |
| `/ai/auth/token`         | POST                | API Key + Secret 换取 JWT |

### 8.3 API Key 管理后台接口（xy-admin，端口 9090）

| 接口                         | 方法     | 说明                  |
| -------------------------- | ------ | ------------------- |
| `/ai/api-key/generate`     | POST   | 生成 API Key + Secret |
| `/ai/api-key/page`         | POST   | 分页查询密钥              |
| `/ai/api-key/{id}/enable`  | PUT    | 启用密钥                |
| `/ai/api-key/{id}/disable` | PUT    | 禁用密钥                |
| `/ai/api-key/{id}`         | DELETE | 删除密钥                |

### 8.4 文档地址

后端启动后访问：

- fg-chat：`http://localhost:9080/doc.html`
- xy-ai：`http://localhost:9070/doc.html`
- xy-admin：`http://localhost:9090/doc.html`

***

## 9. 业务流程

### 9.1 用户注册/登录

1. 前端调用 `/send/code` 获取邮箱验证码。
2. 前端调用 `/auth/register` 提交注册信息。
3. 后端校验验证码、密码一致性，BCrypt 加密后保存 `t_user`。
4. 注册成功后，调用悟空 IM 创建用户并发送系统欢迎消息。
5. 登录时校验账号密码，生成 JWT，刷新悟空 IM Token，返回 IM 路由地址。
6. 前端使用 JWT 调用业务接口，使用 UID/Token 连接悟空 IM WebSocket。

### 9.2 发送好友请求

1. 当前用户在搜索框输入关键词，调用 `/friend/search` 查找用户。
2. 选择目标用户，填写附加消息后调用 `/friend/request/send`。
3. 后端校验：不能添加自己、目标用户存在、非好友、无重复待处理请求。
4. 获取 Redis 分布式锁，落库好友请求记录，向接收者发送通知。
5. 接收者通过通知中心或 IM 弹窗收到好友请求。

### 9.3 处理好友请求

1. 接收者调用 `/friend/request/received` 查看收到的请求。
2. 点击同意/拒绝，调用 `/friend/request/handle`。
3. 后端获取分布式锁，校验请求状态，同意时插入双向好友关系。
4. 向发送者发送同意/拒绝通知。

### 9.4 发送聊天消息

1. 用户在聊天窗口输入文本或插入图片。
2. 图片先经过前端 canvas 压缩，超过 2MB 提示用户。
3. 压缩后的图片上传到七牛云，返回图片 URL。
4. 前端调用 `/message/send`，携带频道 ID、消息类型、内容、`clientMsgNo`。
5. 后端转发到悟空 IM，悟空 IM 投递给目标用户。
6. 发送方先乐观更新本地消息列表，收到 IM 回执后根据 `clientMsgNo` 去重并替换临时 ID。

***

## 10. 开发与测试

### 10.1 代码规范

- 后端遵循阿里巴巴 Java 开发规范，使用 Lombok 简化样板代码。
- 前端使用 TypeScript 严格模式，优先使用 Composition API 与 `<script setup>`。
- 接口返回统一封装为 `R<T>`，异常统一由 `GlobalExceptionHandler` 处理。

### 10.2 测试

- **单元测试**：使用 JUnit 5 + Mockito，覆盖核心业务逻辑。
- **集成测试**：使用 Spring Boot Test + H2 内存数据库 + `@MockBean` 模拟外部依赖。
- **前端构建检查**：`pnpm run build` 执行类型检查与打包。

执行后端测试：

```bash
mvn test -pl chat-api/fg-chat -am -Dnet.bytebuddy.experimental=true
mvn clean test -pl xy-system -am
mvn clean test -pl common-api/xy-ai -am
```

执行前端构建：

```bash
cd y-micro-app/apps/y-chat-sub
pnpm run build
```

***

## 11. 运维与监控

### 11.1 日志

- 后端使用 SLF4J + Logback，按模块配置日志级别。
- 关键操作（登录、发送消息、好友请求）建议记录审计日志。

### 11.2 限流

- 基于 Bucket4j 实现接口限流，防止暴力注册、登录、好友请求刷屏。
- 敏感接口（发送验证码、登录、发送好友请求）建议配置独立限流策略。

### 11.3 安全

- JWT Secret 通过环境变量注入，生产环境长度不低于 256-bit。
- 密码与 API Key Secret 均使用 BCrypt 加密存储，不存明文。
- API Key 采用 `SecureRandom` 生成，API Key 前缀 `xyai_` + 32 字符 Base62，Secret 为 64 字节随机 Base64。
- 图片上传限制类型与大小，后端做二次校验。

### 11.4 常见问题

| 问题               | 可能原因                                              | 解决思路                                                                                                 |
| ---------------- | ------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| 启动报循环依赖          | `SecurityConfig` 与 `JwtAuthenticationFilter` 互相依赖 | 已将 `PasswordEncoder` 抽到独立 `PasswordEncoderConfig`                                                    |
| 聊天界面显示 JSON 字符串  | 悟空 IM payload 为明文 JSON                            | `utils/wk.ts` 已兼容直接 JSON 解析与 Base64 解码                                                               |
| 发送消息后显示两条        | 乐观更新与 IM 回执未去重                                    | `store/chat.ts` 已按 `clientMsgNo` 去重                                                                  |
| 图片超过 2MB         | 未压缩或压缩后仍超限                                        | `utils/image.ts` 已做 canvas 压缩与大小校验                                                                   |
| AI 返回 400（模型不支持） | 传入空 model 或模型名与 endpoint 不匹配                      | 确认 `ai_model_provider.default_model` 已配置，DeepSeek 官方/阿里百炼需使用 `deepseek-v4-pro` 或 `deepseek-v4-flash` |
| API Key 认证失败     | Key 被禁用/过期或 Secret 错误                             | 在 xy-admin 管理页面检查密钥状态，确认 Secret 明文仅生成时返回一次                                                           |
| 通话报 `LiveKit 房间创建失败，code=401` | 后端 `LIVEKIT_API_KEY`/`API_SECRET` 与 LiveKit 服务端 `keys` 不一致 | 对齐两处密钥（`livekit.yaml` 的 `keys` 与后端 `.env`），`RoomServiceClient` 为懒加载单例，改完必须完整重启 JVM |
| 通话报 `could not establish pc connection` | 媒体端口未放行，或 LiveKit 广播的 ICE 候选是内网 IP | 放行 `7881/tcp` 与 `udp_port`（示例 `7882/udp`）；云主机 EIP 通常不支持回环，STUN 校验会失败并回退内网 IP，需在 `livekit.yaml` 显式配置 `node_ip` 并关闭 `use_external_ip` |
| 通话能接通但无画面/无声 | 浏览器未授权摄像头/麦克风，或非 HTTPS 环境 | 检查浏览器权限；非 localhost 的 http 页面禁止采集媒体，需走 https 或桌面端 |
| 通话异常中断后房间长期显示通话中 | 前端异常退出未上报挂断 | 前端异常路径已补发 `/rtc/hangup`，后端定时任务亦会回收 LiveKit 中已无人在场的残留房间 |

***

## 12. 附录

### 12.1 目录结构速查

```
todo-service/
├── common-api/xy-ai          # AI 能力服务（对话、RAG、Agent、API Key 换 token）
├── chat-api/fg-chat          # 聊天主 API
├── customer-api/xy-app       # Todo 任务 API
├── customer-api/xy-blog      # 博客前台 API
├── xy-admin                  # 管理后台 API
├── xy-common                 # 公共模块
├── xy-framework              # 安全、JWT、Web 配置
├── xy-system                 # 系统服务、AI API Key、七牛云、悟空 IM Client
├── y-micro-app/              # 前端 Monorepo
│   ├── apps/main             # qiankun 主应用
│   ├── apps/y-chat-sub       # 聊天子应用
│   ├── apps/y-admin-sub      # 管理后台子应用（含 AI API Key 管理页面）
│   └── packages/             # 公共包
├── deploy/livekit/           # LiveKit SFU 部署配置（docker-compose + livekit.yaml）
├── sql/                      # 数据库脚本
├── docs/                     # 项目文档
└── README.md                 # 项目根说明
```

### 12.2 相关文档

- [项目技术分析报告](./PROJECT_ANALYSIS_REPORT.md)
- [好友与消息通知功能实现文档](./FEATURE_IMPLEMENTATION.md)
- [好友与消息通知功能测试报告](./TEST_REPORT.md)
- [前端悟空 IM 连接代码优化总结](./FRONTEND_IM_REFACTOR_SUMMARY.md)
- [AI 模块架构说明](./common-api/xy-ai/docs/architecture.md)
- [AI 模块使用指南](./common-api/xy-ai/docs/guide.md)
- [AI 模块 API Key 管理技术文档](./common-api/xy-ai/docs/api-key-management.md)
- [AI 模块接口文档](./common-api/xy-ai/docs/api.md)
- [音视频通话设计文档](./docs/rtc/design.md)
- [音视频通话部署文档](./docs/rtc/deployment.md)
- [音视频通话测试报告](./docs/rtc/test-report.md)

### 12.3 联系方式

如有问题，请参考相关模块代码或联系项目维护人员。
