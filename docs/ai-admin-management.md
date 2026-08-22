# AI 模型与知识库后台管理配置功能

## 1. 功能概述

本功能为后台管理系统提供 AI 模型提供者配置、知识库管理、知识库版本控制与权限设置能力。

- **AI 模型管理**：支持模型提供者的增删改查、启用/禁用切换、参数配置（endpoint、apiKey、model、扩展 JSON 等）。
- **知识库管理**：支持知识库的增删改查、启用/禁用切换、Embedding 模型配置。
- **知识库内容管理**：支持文档的增删改查、重新索引；文档分片与向量化通过内部 API Key 调用 xy-ai 服务完成。
- **知识库版本控制**：支持版本的创建、发布、归档、删除。
- **知识库权限设置**：支持按角色或用户维度授予只读（READ）或管理（MANAGE）权限。

## 2. 模块划分

| 模块 | 职责 |
|------|------|
| `xy-common` | 存放 AI 相关实体（`com.xy.common.entity.ai.*`）、枚举（`com.xy.common.enums.ai.*`）以及 DTO/VO（`com.xy.common.dto.ai.*` / `com.xy.common.vo.ai.*`） |
| `xy-system` | 存放 AI 相关 Mapper（`com.xy.system.mapper.ai.*`）与后台管理 Service（`com.xy.system.service.ai.*`），以及调用 xy-ai 的内部客户端 |
| `xy-admin` | 后台管理 Controller，仅做参数校验、权限控制与简单的服务编排 |
| `xy-ai` | 保留 RAG 核心能力（分片、Embedding、向量存储），新增内部 API Key 认证过滤器 |

## 3. 数据表

### 3.1 已有表

- `ai_model_provider`：AI 模型提供者配置表
- `ai_knowledge_base`：知识库表
- `ai_knowledge_document`：知识库文档表
- `ai_knowledge_chunk`：知识库文档片段表

### 3.2 新增表

- `ai_knowledge_base_version`：知识库版本表
- `ai_knowledge_base_permission`：知识库权限表

执行脚本：[sql/ai-admin-config.sql](../sql/ai-admin-config.sql)

## 4. 接口清单

所有后台管理接口均位于 `xy-admin`（端口 9090），统一返回 `R<T>` 结构，受 Spring Security 权限控制。

### 4.1 AI 模型提供者

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/admin/ai/model/page` | `ai:model:list` | 分页查询 |
| GET | `/admin/ai/model/{id}` | `ai:model:query` | 查询详情 |
| POST | `/admin/ai/model` | `ai:model:add` | 新增 |
| PUT | `/admin/ai/model` | `ai:model:edit` | 编辑 |
| DELETE | `/admin/ai/model/{id}` | `ai:model:remove` | 删除 |
| PUT | `/admin/ai/model/{id}/toggle` | `ai:model:edit` | 启用/禁用切换 |

**请求示例 - 新增/编辑模型提供者**

```json
{
  "name": "阿里云百炼 Embedding",
  "code": "dashscope-embedding",
  "type": "OPENAI_COMPATIBLE",
  "endpoint": "https://dashscope.aliyuncs.com/compatible-mode",
  "apiKey": "sk-xxx",
  "model": "text-embedding-v3",
  "priority": 10,
  "enabled": 1,
  "configJson": "{\"temperature\":0.7}"
}
```

### 4.2 知识库

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/admin/ai/knowledge/page` | `ai:knowledge:list` | 分页查询 |
| GET | `/admin/ai/knowledge/{id}` | `ai:knowledge:query` | 查询详情 |
| POST | `/admin/ai/knowledge` | `ai:knowledge:add` | 新增 |
| PUT | `/admin/ai/knowledge` | `ai:knowledge:edit` | 编辑 |
| DELETE | `/admin/ai/knowledge/{id}` | `ai:knowledge:remove` | 删除 |
| PUT | `/admin/ai/knowledge/{id}/toggle` | `ai:knowledge:edit` | 启用/禁用切换 |

**请求示例 - 新增/编辑知识库**

```json
{
  "name": "产品知识库",
  "description": "公司产品与帮助文档",
  "embeddingModel": "text-embedding-v3",
  "providerCode": "dashscope-embedding"
}
```

### 4.3 知识库文档

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/admin/ai/knowledge/{kbId}/document/page` | `ai:knowledge:list` | 分页查询文档 |
| GET | `/admin/ai/knowledge/document/{id}` | `ai:knowledge:query` | 查询文档详情 |
| POST | `/admin/ai/knowledge/document` | `ai:knowledge:add` | 新增文档并触发向量化 |
| PUT | `/admin/ai/knowledge/document` | `ai:knowledge:edit` | 编辑文档并重新向量化 |
| DELETE | `/admin/ai/knowledge/document/{id}` | `ai:knowledge:remove` | 删除文档及向量 |
| POST | `/admin/ai/knowledge/document/{id}/reindex` | `ai:knowledge:edit` | 重新索引文档 |

**请求示例 - 新增/编辑文档**

```json
{
  "kbId": 1,
  "title": "AI 助手介绍",
  "sourceType": "TEXT",
  "sourceUrl": "",
  "content": "我们的 AI 助手支持对话、RAG 检索与工具调用。"
}
```

### 4.4 知识库版本

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/admin/ai/knowledge/{kbId}/version/list` | `ai:knowledge:query` | 查询版本列表 |
| POST | `/admin/ai/knowledge/version` | `ai:knowledge:edit` | 新增/编辑版本 |
| PUT | `/admin/ai/knowledge/version/{id}/publish` | `ai:knowledge:edit` | 发布版本 |
| PUT | `/admin/ai/knowledge/version/{id}/archive` | `ai:knowledge:edit` | 归档版本 |
| DELETE | `/admin/ai/knowledge/version/{id}` | `ai:knowledge:remove` | 删除版本 |

**请求示例 - 新增/编辑版本**

```json
{
  "kbId": 1,
  "version": "v1.0.0",
  "description": "初始版本"
}
```

### 4.5 知识库权限

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/admin/ai/knowledge/{kbId}/permission/list` | `ai:knowledge:query` | 查询权限列表 |
| POST | `/admin/ai/knowledge/permission` | `ai:knowledge:edit` | 授权 |
| DELETE | `/admin/ai/knowledge/permission/{id}` | `ai:knowledge:remove` | 撤销权限 |

**请求示例 - 授权**

```json
{
  "kbId": 1,
  "targetType": "ROLE",
  "targetId": 1,
  "permission": "READ"
}
```

- `targetType`：`ROLE` 或 `USER`
- `permission`：`READ` 或 `MANAGE`

## 5. 权限说明

上述接口使用 `@PreAuthorize("@auth.hasPermi('xxx')")` 进行权限控制。初次部署后，需要在 `sys_menu` 表中维护对应的菜单与权限标识，并在 `sys_role_menu` 中为角色分配权限。

建议新增的菜单权限节点：

```text
ai:model:list / ai:model:query / ai:model:add / ai:model:edit / ai:model:remove
ai:knowledge:list / ai:knowledge:query / ai:knowledge:add / ai:knowledge:edit / ai:knowledge:remove
```

## 6. 内部服务调用

xy-admin 通过 `RestTemplate` 调用 xy-ai 的 `/ai/knowledge/document` 接口完成文档分片与向量化，调用时使用 `X-Internal-Api-Key` 请求头进行认证。

配置项：

```yaml
ai:
  service:
    url: ${AI_SERVICE_URL:http://localhost:9070}
    internal-api-key: ${AI_SERVICE_INTERNAL_API_KEY:}
```

- `AI_SERVICE_URL`：xy-ai 服务地址
- `AI_SERVICE_INTERNAL_API_KEY`：双方约定的内部调用密钥，需在 xy-admin 与 xy-ai 中保持一致

## 7. 关键变更文件

- `xy-admin/src/main/java/com/xy/admin/controller/ai/AiModelAdminController.java`
- `xy-admin/src/main/java/com/xy/admin/controller/ai/AiKnowledgeBaseAdminController.java`
- `xy-common/src/main/java/com/xy/common/entity/ai/*`
- `xy-common/src/main/java/com/xy/common/enums/ai/*`
- `xy-common/src/main/java/com/xy/common/dto/ai/*`
- `xy-common/src/main/java/com/xy/common/vo/ai/*`
- `xy-system/src/main/java/com/xy/system/mapper/ai/*`
- `xy-system/src/main/java/com/xy/system/service/ai/ISysAiModelAdminService.java`
- `xy-system/src/main/java/com/xy/system/service/ai/ISysAiKnowledgeBaseAdminService.java`
- `xy-system/src/main/java/com/xy/system/service/ai/impl/SysAiModelAdminServiceImpl.java`
- `xy-system/src/main/java/com/xy/system/service/ai/impl/SysAiKnowledgeBaseAdminServiceImpl.java`
- `xy-system/src/main/java/com/xy/system/client/ai/AiServiceClient.java`
- `xy-system/src/main/java/com/xy/system/client/ai/impl/AiServiceClientImpl.java`
- `xy-system/src/main/java/com/xy/system/config/ai/AiServiceClientConfig.java`
- `common-api/xy-ai/src/main/java/com/chat/ai/security/InternalApiKeyFilter.java`
- `common-api/xy-ai/src/main/java/com/chat/ai/config/AiServiceProperties.java`
- `sql/ai-admin-config.sql`
