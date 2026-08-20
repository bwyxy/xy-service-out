# AI 模块架构说明

## 1. 模块定位

`xy-ai` 模块为公司统一 AI 能力层，向上层业务提供：

- 多模型对话能力（同步 / 流式）
- 会话与消息历史管理
- RAG 知识库检索增强
- Agent 工具调用框架
- 用户调用配额控制

模块以 **Spring Boot 微服务** 形式独立部署，也可作为 `chat-api` 子模块嵌入现有网关体系。

## 2. 技术栈

| 层级 | 技术 |
|------|------|
| 基础框架 | Spring Boot 3.2.4、Spring AI 1.1.0 |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0（生产）、H2（测试） |
| 缓存 | Redis |
| 向量检索 | Spring AI `SimpleVectorStore`（本地兜底），可扩展至 Milvus / Qdrant |
| 文档解析 | Spring AI PDF / Tika Reader |
| 工具调用 | Spring AI `FunctionToolCallback` |
| 构建工具 | Maven |

## 3. 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                         Controller 层                        │
│   /ai/chat  /ai/chat/stream  /ai/conversation  /ai/provider  │
│   /ai/knowledge                                             │
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
│   ├─ CurrentTimeTool                                         │
│   └─ CalculatorTool                                          │
├─────────────────────────────────────────────────────────────┤
│                         数据层                               │
│   MySQL：ai_model_provider / ai_conversation / ai_message    │
│         ai_knowledge_base / ai_knowledge_document            │
│         ai_knowledge_chunk / ai_tool / ai_user_quota         │
└─────────────────────────────────────────────────────────────┘
```

## 4. 核心流程

### 4.1 同步对话

1. 校验用户配额 `AiQuotaService`。
2. 根据 `providerCode` 或优先级选择模型提供者 `ModelProviderFactory`。
3. 创建 / 获取会话 `AiConversationService`，保存用户消息。
4. 组装历史消息 + 系统提示词 + RAG 上下文 + 可用工具。
5. 调用 `ModelProvider.chat(...)` 获取模型响应。
6. 保存助手消息、更新配额、返回 `ChatResponse`。

### 4.2 流式对话

与同步对话流程一致，但模型返回 `Flux<AiStreamChunk>`，逐段推送 SSE 事件；最后一个 chunk 携带 `done=true`。

### 4.3 RAG 检索

1. 对查询文本调用 `EmbeddingService.embed(query)` 生成向量。
2. `VectorStoreService` 在 `SimpleVectorStore` 中执行相似度检索。
3. 返回 Top-K 文本片段，拼接为上下文注入对话 Prompt。

### 4.4 Agent 工具调用

1. 应用启动时 `ToolRegistry` 从 `ai_tool` 表加载工具定义。
2. 将工具定义转换为 `FunctionToolCallback` 注册到当前模型调用。
3. 模型若返回 `tool_calls`，则调用对应 Spring Bean 的 `ToolExecutor.execute(...)`。
4. 将工具执行结果作为 `tool` 角色消息再次调用模型，生成最终回复。

## 5. 模型提供者抽象

```java
public interface ModelProvider {
    String getCode();
    boolean isAvailable();
    AiModelResponse chat(List<Message> messages, AiChatOptions options, List<ToolDefinition> tools);
    Flux<AiStreamChunk> streamChat(List<Message> messages, AiChatOptions options, List<ToolDefinition> tools);
    void registerTool(String name, String description, String schema, Function<String, String> executor);
}
```

新增模型只需实现 `ModelProvider` 并在 `ModelProviderFactory` 中注册工厂方法，无需改动上层业务代码。

## 6. 扩展点

| 扩展点 | 说明 |
|--------|------|
| 新增模型提供者 | 实现 `ModelProvider` + 在 `ModelProviderFactory` 注册 |
| 新增 Agent 工具 | 实现 `ToolExecutor` 并注册为 Spring Bean，然后在 `ai_tool` 表录入 |
| 替换向量数据库 | 替换 `AiConfig#vectorStore` 的 Bean 实现 |
| 自定义配额策略 | 扩展 `AiQuotaService` |
