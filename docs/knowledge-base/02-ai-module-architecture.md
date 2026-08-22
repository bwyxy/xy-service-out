# xy-ai 模块架构与 RAG 双轨配置

## 模块定位

common-api/xy-ai 是 AI 能力服务，端口 9070，负责：

- 多模型 AI 对话（同步 / SSE 流式）
- RAG 知识库检索与增强
- API Key 换取临时 token
- 会话与消息管理

## 核心入口

- 启动类：com.chat.ai.XyAiApplication
- 对话接口：com.chat.ai.controller.AiController
- 对话实现：com.chat.ai.service.impl.AiChatServiceImpl
- 知识库实现：com.chat.ai.service.impl.AiKnowledgeBaseServiceImpl
- 向量服务：com.chat.ai.rag.VectorStoreService
- Embedding 工厂：com.chat.ai.rag.EmbeddingModelFactory

## 对话流程

1. 消耗用户配额
2. 解析/创建会话
3. 保存用户消息
4. RAG 上下文增强（若传 knowledgeBaseIds）
5. 调用大模型
6. 保存助手消息

## RAG 流程

- 文档分片：com.chat.ai.rag.DocumentChunker
- 向量生成：通过 EmbeddingModelFactory 获取 EmbeddingModel
- 向量存储：MySQL 表 ai_knowledge_chunk
- 向量检索：余弦相似度计算

## 双轨 Embedding 配置

系统支持两种 Embedding 配置方式，优先级：知识库独立配置 > 全局默认配置。

### 1. 全局默认配置

在 .env 中配置：

```properties
EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
EMBEDDING_API_KEY=sk-你的dashscope-key
EMBEDDING_MODEL=text-embedding-v3
```

对应 application.yml：

```yaml
ai:
  embedding:
    api-key: ${EMBEDDING_API_KEY:${OPENAI_API_KEY:}}
    base-url: ${EMBEDDING_BASE_URL:${OPENAI_BASE_URL:}}
    model: ${EMBEDDING_MODEL:${OPENAI_MODEL:}}
```

当 ai_knowledge_base.provider_code 为空时，走全局默认模型。

### 2. 按知识库独立配置

在 ai_model_provider 表中创建独立的 Embedding 提供者，例如：

```sql
INSERT INTO ai_model_provider (name, code, type, endpoint, api_key, model, enabled)
VALUES (
    '阿里云 Embedding',
    'dashscope-embedding',
    'OPENAI_COMPATIBLE',
    'https://dashscope.aliyuncs.com/compatible-mode',
    'sk-你的dashscope-key',
    'text-embedding-v3',
    1
);
```

然后在 ai_knowledge_base 表中指定：

```sql
UPDATE ai_knowledge_base
SET provider_code = 'dashscope-embedding',
    embedding_model = 'text-embedding-v3'
WHERE id = 1;
```

## 重要约束

- 同一个知识库的入库和检索必须使用同一个 embedding 模型，否则向量无法对齐。
- DeepSeek 没有 Embedding 接口，不能用于 RAG 向量模型。
- 单个知识库检索失败仅记录 warn 日志，不阻断整体对话流程。
- 直接 SQL 插入的文档无向量数据，无法用于 RAG，必须通过 API 导入以触发分片和向量生成。

## 常见错误

- 401 Invalid API-key：Embedding Key 错误或服务不对。
- 404：Endpoint 末尾多余 /v1，或使用了不支持 Embedding 的模型（如 DeepSeek）。
