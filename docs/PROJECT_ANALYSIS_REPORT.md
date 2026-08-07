# 项目技术分析报告

## 1. 项目概述

本项目为一个多模块、微前端架构的综合服务平台。项目包含：

- 后端：基于 Spring Boot 3.2.4 的多模块 Maven 工程
- 前端：基于 Vue 3 + TypeScript + qiankun 的微前端 Monorepo 工程
- 即时通讯：集成悟空 IM（WukongIM）作为消息通道

## 2. 项目架构设计

### 2.1 后端模块架构

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

架构特点：

- 多模块 Maven 工程，按业务领域垂直拆分
- `xy-common` 被所有业务模块依赖，提供统一实体、响应封装、Feign Client
- `xy-framework` 提供安全、JWT、限流、全局异常等横切能力
- 聊天服务 `fg-chat` 通过 Feign 调用 `xy-system` 中的 `WKClientService` 与悟空 IM 交互
- 各业务服务可独立部署，通过不同端口区分

### 2.2 前端模块架构

```
y-micro-app (pnpm workspace)
├── apps/
│   ├── main           # qiankun 主应用（基座，端口 5000）
│   ├── y-chat-sub     # 聊天子应用（端口 9001）
│   └── y-admin-sub    # 管理后台子应用（端口 9002）
├── packages/
│   ├── xy-utils       # 公共工具库
│   └── xy-qiankun-communication  # 微前端通信库
```

架构特点：

- 微前端基座 `main` 负责注册、加载子应用和全局状态管理
- 子应用通过 `vite-plugin-qiankun` 接入 qiankun 生命周期
- `@xy/qiankun-communication` 封装基于 `initGlobalState` 的类型安全通信
- 各子应用可独立运行，便于开发调试

## 3. 核心功能模块说明

### 3.1 认证与授权模块（xy-framework + fg-chat）

- 基于 Spring Security 6 + JWT 的无状态认证
- `JwtAuthenticationFilter` 从 `token` 请求头解析 JWT
- `JwtTokenProvider` 生成/验证 Token
- 自定义 `ChatUserDetails` 保存当前登录用户 UID
- `@Anonymous` 注解 + `PermitAllUrlProperties` 配置匿名白名单

### 3.2 用户模块（xy-system + fg-chat）

- 用户实体 `t_user`：UID、昵称、密码、头像、手机号、邮箱、状态、性别、微信 OpenID
- 提供用户信息查询接口 `/user/{uId}`
- 注册时校验邮箱验证码，密码使用 BCrypt 加密

### 3.3 即时通讯模块（chat-api/fg-chat）

- 集成悟空 IM，通过 Feign Client `WuKongClient` 调用悟空 IM 的 HTTP API
- 功能覆盖：用户 Token 刷新、IM 路由获取、消息发送、会话同步、历史消息同步
- 前端通过 `easyjssdk` 连接悟空 IM WebSocket 进行实时消息收发

### 3.4 会话与消息模块

- `ConversationController` + `ConversationServiceImpl`：同步悟空 IM 会话列表，并补充本地用户信息/群组信息
- `MessageController` + `MessageServiceImpl`：同步频道历史消息

### 3.5 Todo/任务模块（customer-api/xy-app）

- 任务、标签、协作者、微信登录等功能
- 使用 MyBatis-Plus 进行 CRUD

### 3.6 博客/内容模块（xy-blog + xy-admin）

- 文章、分类、标签、评论、菜单、配置等 CMS 能力

## 4. 技术栈组成

### 4.1 后端技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| 基础框架 | Spring Boot | 3.2.4 |
| 安全框架 | Spring Security | 6.x |
| 服务间调用 | Spring Cloud OpenFeign | 2023.0.3 |
| ORM 框架 | MyBatis-Plus | 3.5.5 / 3.5.9 |
| 数据库 | MySQL | 8.0.33 |
| 连接池 | Druid | 1.2.27 |
| 缓存 | Redis（Bucket4j-redis 依赖） | - |
| 限流 | Bucket4j | 8.0.1 |
| 工具库 | Hutool、Apache Commons、Fastjson | - |
| 文档 | Swagger / Knife4j（doc.html） | - |
| 即时通讯 | 悟空 IM（WukongIM） | - |
| 构建工具 | Maven | - |
| JDK | Java | 17 |

### 4.2 前端技术栈

| 类别 | 技术 | 说明 |
|---|---|---|
| 框架 | Vue 3 | Composition API |
| 语言 | TypeScript | - |
| 构建工具 | Vite 7 | - |
| 微前端 | qiankun + vite-plugin-qiankun | - |
| 状态管理 | Pinia | - |
| 路由 | Vue Router 5 | - |
| UI 组件库 | Element Plus | - |
| CSS 预处理 | Sass / SCSS | - |
| 工具库 | @vueuse/core, easyjssdk | 悟空 IM SDK |
| 包管理 | pnpm workspace | catalog 统一版本 |

## 5. 数据模型结构

### 5.1 聊天模块核心表

#### t_user（用户表）

| 字段 | 类型 | 说明 |
|---|---|---|
| u_id | bigint PK AI | 用户 UID |
| password | varchar(500) | BCrypt 密码 |
| nickname | varchar(50) | 昵称 |
| avatar | varchar(255) | 头像 URL |
| phone | varchar(11) | 手机号，唯一 |
| email | varchar(50) | 邮箱，唯一 |
| status | char(1) | 0-正常 1-禁用 |
| sex | char(1) | 0-未知 1-男 2-女 |
| wx_open_id | varchar(255) | 微信 OpenID |
| is_bind_chat | char(1) | 是否绑定聊天应用 |
| create_time / update_time | datetime | 创建/更新时间 |

#### t_group（群组表）

| 字段 | 类型 | 说明 |
|---|---|---|
| group_id | bigint PK AI | 群组 ID |
| name | varchar(64) | 群组名称 |
| avatar | varchar(255) | 群头像 |
| owner_uid | bigint | 群主 ID |
| is_ban / disband | int | 是否禁言/解散 |
| create_by / update_by | bigint | 创建/更新者 |
| create_time / update_time | datetime | 时间 |

#### t_group_member（群组成员表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK AI | ID |
| group_id | bigint | 群组 ID |
| u_id | bigint | 用户 ID |
| role | int | 0-成员 1-管理员 |
| status | char(1) | 0-正常 1-已退出 |
| joined_at | datetime | 加入时间 |

#### t_friend（好友表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK AI | ID |
| u_id | bigint | 用户 ID |
| friend_id | bigint | 好友用户 ID |
| status | char(1) | 0-正常 1-拉黑 |
| create_time / update_time | datetime | 时间 |

> 说明：当前 `t_friend` 仅存储最终好友关系，缺少好友请求中间状态。

## 6. 现有 API 接口文档

### 6.1 fg-chat（端口 9080）

| 接口 | 方法 | 说明 |
|---|---|---|
| `/auth/register` | POST | 用户注册 |
| `/chat/login` | POST | 用户登录，返回 JWT + IM 地址 |
| `/auth/userinfo` | POST | 获取当前登录用户信息 |
| `/send/code` | POST | 发送邮箱验证码 |
| `/user/{uId}` | GET | 查询用户信息 |
| `/api/chat/update/token` | POST | 更新悟空 IM Token |
| `/api/chat/login` | POST | 获取悟空 IM 连接凭据 |
| `/api/chat/token` | POST | 获取悟空 IM Token |
| `/api/chat/group/create` | POST | 创建群聊频道 |
| `/api/chat/group/join` | POST | 加入群聊 |
| `/api/chat/messages/sync` | POST | 同步消息 |
| `/conversation/list` | POST | 获取会话列表 |
| `/message/list` | POST | 获取历史消息 |

## 7. 现有业务流程

### 7.1 用户注册/登录流程

1. 前端调用 `/send/code` 获取邮箱验证码
2. 前端调用 `/auth/register` 提交注册信息
3. 后端校验验证码、密码一致性，BCrypt 加密后保存 `t_user`
4. 注册成功后，调用悟空 IM 创建用户并发送系统欢迎消息
5. 登录时校验账号密码，生成 JWT，刷新悟空 IM Token，返回 IM 路由地址
6. 前端使用 JWT 调用业务接口，使用 UID/Token 连接悟空 IM WebSocket

### 7.2 聊天消息流程

1. 用户登录后获取 IM 连接凭据（wsUrl + token）
2. 前端 `easyjssdk` 连接悟空 IM WebSocket
3. 发送消息时，消息经悟空 IM 服务端投递给目标用户/群组
4. 会话列表和历史消息通过后端 `/conversation/list`、`/message/list` 同步
5. 后端通过悟空 IM HTTP API 获取原始会话/消息数据，并补充本地用户/群组元数据

## 8. 潜在技术风险及优化建议

### 8.1 架构与代码层面

| 风险 | 说明 | 优化建议 |
|---|---|---|
| 公共模块膨胀 | `xy-common` 既包含实体、工具，又包含 Feign Client 和 VO，职责边界模糊 | 拆分为 `xy-common-core`、`xy-common-entity`、`xy-common-client` 等子模块 |
| 重复配置 | 多个模块存在 `Bucket4jConfig`、`WebConfig` 等重复配置 | 统一下沉到 `xy-framework` |
| 魔法值与字符串 | 多处使用硬编码的 `channelType`、`status` 等魔法值 | 使用枚举类统一管理 |
| 异常处理不统一 | 部分控制器直接抛出 `RuntimeException`，部分返回 `R.error` | 统一通过 `GlobalExceptionHandler` 处理 |
| 缺少单元测试 | 当前测试目录基本为空 | 为核心业务逻辑补充单元测试和集成测试 |

### 8.2 安全与性能

| 风险 | 说明 | 优化建议 |
|---|---|---|
| JWT Secret 硬编码风险 | `application.yml` 中默认 secret 使用占位符，但示例值较短 | 强制使用环境变量注入，且密钥长度 ≥ 256-bit |
| 悟空 IM Token 管理 | Token 刷新逻辑与业务登录耦合 | 独立 Token 刷新定时任务，避免登录失败 |
| 限流策略未全覆盖 | Bucket4j 配置存在但未在 fg-chat 中显式启用 | 为好友请求、登录等敏感接口配置限流 |
| 并发控制缺失 | 好友请求、同意/拒绝等操作存在竞态条件 | 引入数据库唯一索引 + Redis 分布式锁 |

### 8.3 数据与业务

| 风险 | 说明 | 优化建议 |
|---|---|---|
| `t_friend` 缺少请求状态 | 当前好友表直接表示最终关系，无法表达“待确认”等中间态 | 新增 `t_friend_request` 好友请求表 |
| 通知系统缺失 | 好友请求、同意等事件无系统级通知与未读管理 | 新增 `t_notification` 通知表，并通过悟空 IM 推送实时通知 |
| 消息持久化依赖外部 IM | 历史消息完全依赖悟空 IM，存在数据不可控风险 | 关键消息同步落库，作为备份与审计 |
| 好友关系双向维护 | `t_friend` 目前仅单向存储 | 同意好友时同时插入双向记录或约定小 UID 在前 |

## 9. 本次功能扩展方向

基于上述分析，建议在 `fg-chat` 模块扩展：

1. **好友功能**：`t_friend_request` 表 + 好友搜索/请求/同意/拒绝 API
2. **通知系统**：`t_notification` 表 + 通知中心 API + 悟空 IM 实时推送
3. **并发控制**：数据库唯一索引 + Redis 分布式锁 + 幂等 Token
