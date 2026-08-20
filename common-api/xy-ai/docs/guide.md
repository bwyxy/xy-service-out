# AI 模块使用指南

## 1. 环境准备

### 1.1 数据库

执行 `sql/ai.sql` 创建 AI 模块所需的表和初始数据：

```bash
mysql -u root -p your_database < sql/ai.sql
```

### 1.2 环境变量

生产环境建议通过环境变量注入模型密钥：

| 变量 | 说明 | 示例 |
|------|------|------|
| `OPENAI_API_KEY` | OpenAI 兼容接口 API Key | sk-xxx |
| `OPENAI_BASE_URL` | 接口基础地址 | https://api.openai.com/v1 |
| `OPENAI_MODEL` | 默认模型 | gpt-4o |

也可直接通过管理接口 `/ai/provider` 维护多条模型配置。

### 1.3 启动服务

```bash
mvn -pl chat-api/xy-ai -am spring-boot:run
```

默认端口：`9070`。

## 2. 快速开始

### 2.1 发起一次对话

```bash
curl -X POST http://localhost:9070/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "userType": "CLIENT",
    "message": "请介绍一下 Spring AI"
  }'
```

### 2.2 流式对话

```bash
curl -N -X POST http://localhost:9070/ai/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "userType": "CLIENT",
    "message": "你好"
  }'
```

前端需使用 `EventSource` 或 axios 的 `onDownloadProgress` 读取 SSE 流。

### 2.3 创建知识库并检索

```bash
# 创建知识库
curl -X POST http://localhost:9070/ai/knowledge/base \
  -H "Content-Type: application/json" \
  -d '{"name":"产品手册","description":"公司产品知识库"}'

# 假设返回 kbId 为 1
# 添加文档
curl -X POST http://localhost:9070/ai/knowledge/document \
  -H "Content-Type: application/json" \
  -d '{
    "kbId": 1,
    "title": "快速入门",
    "sourceType": "TEXT",
    "content": "Spring AI 是 Spring 框架的 AI 应用开发扩展..."
  }'

# 检索
curl "http://localhost:9070/ai/knowledge/retrieve?kbId=1&query=Spring%20AI&topK=3"
```

## 3. 在对话中使用 RAG

在 `ChatRequest` 中传入 `knowledgeBaseIds`，系统会在请求模型前自动检索相关知识片段并拼接到系统提示词：

```json
{
  "userId": 1,
  "userType": "CLIENT",
  "message": "如何配置向量数据库？",
  "knowledgeBaseIds": [1]
}
```

## 4. 在对话中使用 Agent 工具

设置 `enableTools=true`，模型会根据需要调用已注册工具：

```json
{
  "userId": 1,
  "userType": "CLIENT",
  "message": "现在几点了？1+2*3 等于多少？",
  "enableTools": true
}
```

系统内置工具：

- `current_time`：返回当前时间。
- `calculator`：安全计算基础数学表达式。

## 5. 新增模型提供者

1. 实现 `ModelProvider` 接口。
2. 在 `ModelProviderFactory#buildProvider` 中根据 `ProviderType` 创建实例。
3. 通过 `/ai/provider` 接口录入数据库，设置 `enabled=1` 即可生效。

## 6. 新增 Agent 工具

1. 创建类实现 `ToolExecutor` 并标注 `@Component("yourToolBeanName")`。
2. 在 `ai_tool` 表中插入记录：

```sql
INSERT INTO ai_tool (name, code, description, parameters_schema, implementation_bean, enabled, create_time, update_time)
VALUES (
  '查询天气',
  'weather_query',
  '根据城市名查询天气。',
  '{"type":"object","properties":{"city":{"type":"string","description":"城市名"}},"required":["city"]}',
  'weatherQueryTool',
  1,
  NOW(),
  NOW()
);
```

3. 重启服务后，`ToolRegistry` 会自动加载新工具。

## 7. 常见问题

### 7.1 流式接口无输出

- 检查前端是否正确订阅 SSE。
- 检查模型端点是否支持流式输出。
- 查看日志中是否有 `流式对话异常` 提示。

### 7.2 知识库检索为空

- 确认文档已处理完成（`ai_knowledge_document.status=1`）。
- 确认 `EmbeddingService` 可用（已配置 Embedding 模型或 fallback 到 `EmptyEmbeddingModel`）。
- 对于生产环境，建议接入专用向量数据库。

### 7.3 配额不足

`AiQuotaService` 默认每日限制 100 次。可通过修改 `ai_user_quota` 表的 `daily_limit` 调整。
