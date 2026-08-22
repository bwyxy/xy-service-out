# xy-service 数据库表结构

## SQL 脚本位置

- sql/ai.sql：AI 模块
- sql/chat.sql：聊天模块
- sql/todo.sql：系统 RBAC + Todo 任务
- sql/blog.sql：博客 CMS

## AI 模块核心表

### ai_model_provider

| 字段 | 说明 |
|---|---|
| id | 主键 |
| name | 提供者名称 |
| code | 唯一编码，如 openai/deepseek/dashscope |
| type | 类型：OPENAI_COMPATIBLE / QIANFAN |
| endpoint | API 基础地址 |
| api_key | API Key |
| model | 默认模型 |
| priority | 优先级 |
| enabled | 是否启用 |

### ai_knowledge_base

| 字段 | 说明 |
|---|---|
| id | 主键 |
| name | 知识库名称 |
| description | 描述 |
| embedding_model | 指定 Embedding 模型 |
| provider_code | 关联 ai_model_provider.code，指定 Embedding 提供者 |
| status | 状态 |

### ai_knowledge_document

| 字段 | 说明 |
|---|---|
| id | 主键 |
| kb_id | 所属知识库 |
| title | 文档标题 |
| content | 文档内容 |
| status | 处理状态 |

### ai_knowledge_chunk

| 字段 | 说明 |
|---|---|
| id | 主键 |
| document_id | 所属文档 |
| content | 分片内容 |
| embedding | 向量 JSON |

### ai_conversation

| 字段 | 说明 |
|---|---|
| id | 主键 |
| user_id | 用户 |
| title | 会话标题 |
| model | 使用模型 |

### ai_message

| 字段 | 说明 |
|---|---|
| id | 主键 |
| conversation_id | 会话 |
| role | user / assistant / system |
| content | 内容 |

### ai_api_key / sys_ai_api_key

管理 AI API Key，用于外部应用通过 API Key 换取临时 JWT token。

## 聊天模块核心表

- chat_user：用户信息
- chat_friend_request：好友请求
- chat_friend：好友关系
- chat_group：群组
- chat_group_member：群成员
- chat_message：消息
- chat_conversation：会话
- chat_notification：通知

## 系统 RBAC 核心表

- sys_user
- sys_role
- sys_menu
- sys_user_role
- sys_role_menu

## Todo 模块核心表

- todo_task
- todo_tag
- todo_task_tag
- todo_collaborator

## 博客模块核心表

- blog_article
- blog_category
- blog_tag
- blog_article_tag
- blog_comment
