# xy-service 项目整体概览

## 项目定位

xy-service（内部代号"忆"）是一个综合性服务平台，基于 Spring Boot 3 后端 + Vue 3 微前端构建，集成悟空 IM（WukongIM）提供实时聊天能力。

## 后端模块划分

| 模块 | 路径 | 端口 | 核心职责 |
|---|---|---|---|
| xy-common | xy-common/ | - | 公共实体、工具类、枚举、Feign Client、统一响应 R<T>、JWT 工具、.env 加载器 |
| xy-framework | xy-framework/ | - | Spring Security、JWT 过滤器、全局异常、限流、日志切面 |
| xy-system | xy-system/ | 9090 | 用户/权限/菜单、AI API Key、七牛云、悟空 IM Client |
| xy-admin | xy-admin/ | 9090 | 管理后台 REST API：用户、角色、菜单、内容、AI API Key 管理 |
| common-api/xy-ai | common-api/xy-ai/ | 9070 | AI 能力服务：多模型对话、RAG 检索、Agent、API Key 换 token |
| customer-api/fg-chat | customer-api/fg-chat/ | 9080 | 飞鸽聊天：单聊/群聊、好友关系、会话、消息、通知 |
| customer-api/xy-app | customer-api/xy-app/ | 9100 | Todo 任务、标签、协作者、微信登录 |
| customer-api/xy-blog | customer-api/xy-blog/ | - | 博客前台：文章、分类、标签、评论 |

## 前端结构

前端位于 y-micro-app/，采用 pnpm workspace + qiankun 微前端：

- 主应用（基座）
- 聊天子应用
- 管理后台子应用

## 核心技术栈

- Spring Boot 3.2.4 + Java 17
- Spring Security 6 + JWT 无状态认证
- Spring Cloud OpenFeign 2023.0.3
- MyBatis-Plus 3.5.5/3.5.9 + MySQL 8.0.33 + Druid
- Redis + Bucket4j 限流
- Spring AI 1.1.0（OpenAI 兼容 / 阿里云百炼 / DeepSeek）
- 悟空 IM 实时消息
- Vue 3 + TypeScript + Vite 7 + qiankun + Pinia

## 工程约定

1. 统一返回 R<T>，异常由 GlobalExceptionHandler 统一处理。
2. 匿名白名单通过 @Anonymous 注解 + PermitAllUrlProperties 配置。
3. MyBatis-Plus 雪花算法 assign_id，逻辑删除字段统一为 deleted。
4. JWT Secret 通过环境变量注入，密码/API Key Secret 使用 BCrypt 加密。
5. 敏感接口使用 Bucket4j 限流。
6. 配置从 .env 加载，模板见 .env.example。
