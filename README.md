
# 

## 1. 项目概述

本项目是一个基于 **Spring Boot 3** 后端与 **Vue 3 + qiankun 微前端** 架构的综合性服务平台，集成 **悟空 IM（WukongIM）** 提供实时聊天能力。系统采用多模块 Maven 与 Monorepo 前端组织方式，支持业务模块独立开发、独立部署。

### 1.1 主要业务

- **用户中心**：注册、登录、用户信息管理、头像上传。
- **即时通讯（飞鸽聊天）**：单聊、群聊、好友关系、会话列表、历史消息、富文本输入（文本/表情/图片）。
- **消息通知**：好友请求、好友同意、系统通知的实时推送与通知中心。
- **内容管理（博客/CMS）**：文章、分类、标签、评论、菜单、配置。
- **任务协作（Todo）**：任务、标签、协作者、微信登录。
- **管理后台**：用户、权限、菜单、内容审核等后台管理能力。

### 1.2 项目定位

- 后端按业务领域垂直拆分，便于团队并行开发与独立演进。
- 前端采用 qiankun 微前端，主应用作为基座，子应用可独立运行与发布。
- 聊天模块通过后端 HTTP 接口转发消息，便于后续接入内容安全、敏感词、图片审核等能力。

---

## 2. 系统架构

### 2.1 后端架构

```
xy-service (parent)
├── xy-common          # 公共实体、工具类、枚举、Feign Client、异常定义
├── xy-framework       # 安全、JWT、全局异常、限流、Web 配置
├── xy-system          # 系统服务：用户、权限、菜单、文章、博客、七牛云、悟空 IM Client
├── xy-admin           # 管理后台 REST API
├── chat-api           # 聊天模块父工程
│   ├── fg-chat        # 飞鸽聊天主 API（端口 9080）
│   └── xy-ai          # AI 模块
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

---

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

| 功能 | 说明 |
|------|------|
| 单聊/群聊 | 基于悟空 IM 频道（channel）实现 |
| 好友关系 | 搜索用户、发送请求、同意/拒绝、双向好友关系 |
| 会话列表 | 同步悟空 IM 会话，补充昵称/头像，支持未读角标 |
| 历史消息 | 选择联系人后从后端拉取历史消息 |
| 富文本输入 | 支持文本、Emoji、图片（拖拽/粘贴/上传），图片自动压缩 |
| 消息去重 | 基于 `clientMsgNo` / `messageId` 防止重复渲染 |

### 3.4 消息通知

- 好友请求、好友同意、系统通知生成通知记录。
- 通过悟空 IM 系统消息实时推送到接收方。
- 前端通知中心展示未读数量、通知列表，支持单条/全部已读。

### 3.5 并发控制

- 针对发送好友请求、处理好友请求等关键操作使用 Redis 分布式锁。
- 数据库唯一索引兜底，防止重复请求与重复好友关系。

---

## 4. 技术栈

### 4.1 后端

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.2.4 |
| 安全框架 | Spring Security | 6.x |
| 服务间调用 | Spring Cloud OpenFeign | 2023.0.3 |
| ORM 框架 | MyBatis-Plus | 3.5.5 / 3.5.9 |
| 数据库 | MySQL | 8.0.33 |
| 连接池 | Druid | 1.2.27 |
| 缓存 | Redis | - |
| 限流 | Bucket4j | 8.0.1 |
| 文档 | Swagger / Knife4j | - |
| 即时通讯 | 悟空 IM（WukongIM） | - |
| 构建工具 | Maven | - |
| JDK | Java | 17 |

### 4.2 前端

| 类别 | 技术 | 说明 |
|------|------|------|
| 框架 | Vue 3 | Composition API |
| 语言 | TypeScript | - |
| 构建工具 | Vite 7 | - |
| 微前端 | qiankun + vite-plugin-qiankun | - |
| 状态管理 | Pinia | - |
| 路由 | Vue Router 5 | - |
| UI 组件库 | Element Plus | - |
| CSS 预处理 | Sass / SCSS | - |
| IM SDK | easyjssdk | 悟空 IM 官方 SDK |
| 包管理 | pnpm workspace | catalog 统一版本 |

---

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

- `chat.sql`：聊天模块表结构（用户、好友、好友请求、通知、群组等）
- `blog.sql`：博客/CMS 模块表结构
- `todo.sql`：Todo 任务模块表结构
- `comic.sql`：其他业务表结构

### 5.3 外部依赖

- **七牛云**：用于图片/头像上传，需在配置中填写 `AccessKey`、`SecretKey`、`Bucket`、`Domain`。
- **悟空 IM**：用于实时消息收发，需配置 `appId`、`appKey`、`apiUrl`、`wsUrl`。
- **邮箱服务**：用于发送注册验证码，需配置 SMTP 信息。

---

## 6. 快速开始

### 6.1 后端启动

1. 创建数据库并执行 `sql/` 目录下的初始化脚本。
2. 复制 `.env.example` 为 `.env`，填写数据库、Redis、七牛云、悟空 IM、邮箱等配置。
3. 编译项目：

```bash
mvn clean compile -DskipTests
```

4. 启动 `fg-chat`：

```bash
mvn spring-boot:run -pl chat-api/fg-chat -am
```

5. 启动其他业务模块（按需）：

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

2. 启动主应用：

```bash
cd apps/main
pnpm run dev
```

3. 启动聊天子应用：

```bash
cd apps/y-chat-sub
pnpm run dev
```

4. 启动管理后台子应用（按需）：

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

---

## 7. 数据库设计

### 7.1 聊天模块核心表

| 表名 | 说明 |
|------|------|
| `t_user` | 聊天用户表 |
| `t_friend` | 好友关系表 |
| `t_friend_request` | 好友请求表 |
| `t_notification` | 通知表 |
| `t_group` | 群组表 |
| `t_group_member` | 群组成员表 |

### 7.2 关键约束

- `t_friend`：`unique index uk_user_friend (u_id, friend_id)`
- `t_friend_request`：`unique index uk_from_to (from_uid, to_uid)`
- `t_notification`：`index idx_user_read (u_id, is_read)`

---

## 8. API 接口概览

### 8.1 聊天模块（fg-chat，端口 9080）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/auth/register` | POST | 用户注册 |
| `/chat/login` | POST | 用户登录 |
| `/auth/userinfo` | POST | 获取当前用户信息 |
| `/send/code` | POST | 发送邮箱验证码 |
| `/user/{uId}` | GET | 查询用户信息 |
| `/friend/search` | GET | 搜索用户 |
| `/friend/request/send` | POST | 发送好友请求 |
| `/friend/request/handle` | POST | 处理好友请求 |
| `/friend/list` | GET | 好友列表 |
| `/notification/list` | GET | 通知列表 |
| `/notification/unread/count` | GET | 未读通知数量 |
| `/notification/read/{id}` | POST | 单条已读 |
| `/conversation/list` | POST | 会话列表 |
| `/message/list` | POST | 历史消息 |
| `/message/send` | POST | 发送消息 |
| `/upload/image` | POST | 上传图片 |

### 8.2 文档地址

后端启动后访问：

```
http://localhost:9080/doc.html
```

---

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

---

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
```

执行前端构建：

```bash
cd y-micro-app/apps/y-chat-sub
pnpm run build
```

---

## 11. 运维与监控

### 11.1 日志

- 后端使用 SLF4J + Logback，按模块配置日志级别。
- 关键操作（登录、发送消息、好友请求）建议记录审计日志。

### 11.2 限流

- 基于 Bucket4j 实现接口限流，防止暴力注册、登录、好友请求刷屏。
- 敏感接口（发送验证码、登录、发送好友请求）建议配置独立限流策略。

### 11.3 安全

- JWT Secret 通过环境变量注入，生产环境长度不低于 256-bit。
- 密码使用 BCrypt 加密存储。
- 图片上传限制类型与大小，后端做二次校验。

### 11.4 常见问题

| 问题 | 可能原因 | 解决思路 |
|------|----------|----------|
| 启动报循环依赖 | `SecurityConfig` 与 `JwtAuthenticationFilter` 互相依赖 | 已将 `PasswordEncoder` 抽到独立 `PasswordEncoderConfig` |
| 聊天界面显示 JSON 字符串 | 悟空 IM payload 为明文 JSON | `utils/wk.ts` 已兼容直接 JSON 解析与 Base64 解码 |
| 发送消息后显示两条 | 乐观更新与 IM 回执未去重 | `store/chat.ts` 已按 `clientMsgNo` 去重 |
| 图片超过 2MB | 未压缩或压缩后仍超限 | `utils/image.ts` 已做 canvas 压缩与大小校验 |

---

## 12. 附录

### 12.1 目录结构速查

```
todo-service/
├── chat-api/fg-chat          # 聊天主 API
├── customer-api/xy-app       # Todo 任务 API
├── customer-api/xy-blog      # 博客前台 API
├── xy-admin                  # 管理后台 API
├── xy-common                 # 公共模块
├── xy-framework              # 安全、JWT、Web 配置
├── xy-system                 # 系统服务、七牛云、悟空 IM Client
├── y-micro-app/              # 前端 Monorepo
│   ├── apps/main             # qiankun 主应用
│   ├── apps/y-chat-sub       # 聊天子应用
│   ├── apps/y-admin-sub      # 管理后台子应用
│   └── packages/             # 公共包
├── sql/                      # 数据库脚本
├── docs/                     # 项目文档
└── README.md                 # 项目根说明
```

### 12.2 相关文档

- [项目技术分析报告](./PROJECT_ANALYSIS_REPORT.md)
- [好友与消息通知功能实现文档](./FEATURE_IMPLEMENTATION.md)
- [好友与消息通知功能测试报告](./TEST_REPORT.md)
- [前端悟空 IM 连接代码优化总结](../FRONTEND_IM_REFACTOR_SUMMARY.md)

### 12.3 联系方式

如有问题，请参考相关模块代码或联系项目维护人员。
