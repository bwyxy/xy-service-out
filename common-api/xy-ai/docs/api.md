# AI 模块 API 接口文档

基础路径：`/ai`

响应统一包装为 `R<T>`，结构如下：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

---

## 1. 对话接口

### 1.1 同步对话

- **URL**：`POST /ai/chat`
- **Content-Type**：`application/json`

#### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| conversationId | Long | 否 | 已有会话 ID，为空则新建 |
| userId | Long | 是 | 用户 ID |
| userType | String | 是 | `CLIENT` / `ADMIN` |
| message | String | 是 | 用户输入 |
| providerCode | String | 否 | 指定模型提供者编码 |
| model | String | 否 | 指定模型名称 |
| systemPrompt | String | 否 | 系统提示词 |
| stream | Boolean | 否 | 是否流式（此处固定为 false）|
| knowledgeBaseIds | List<Long> | 否 | 关联知识库 ID 列表 |
| enableTools | Boolean | 否 | 是否启用 Agent 工具 |

#### 请求示例

```json
{
  "userId": 1,
  "userType": "CLIENT",
  "message": "你好",
  "enableTools": true
}
```

#### 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "conversationId": 100,
    "messageId": 101,
    "content": "你好！有什么可以帮您的？",
    "model": "deepseek-v4-flash",
    "done": true,
    "errorMsg": null
  }
}
```

### 1.2 流式对话

- **URL**：`POST /ai/chat/stream`
- **Content-Type**：`application/json`
- **Produces**：`text/event-stream`

请求参数与同步对话一致。返回 SSE 流，每行一个 `AiStreamChunk`：

```
data:{"content":"你好","done":false,"usage":null,"errorMsg":null}

data:{"content":"！","done":false,"usage":null,"errorMsg":null}

data:{"content":"","done":true,"usage":{"promptTokens":10,"completionTokens":5,"totalTokens":15},"errorMsg":null}
```

---

## 2. 会话管理接口

基础路径：`/ai/conversation`

### 2.1 分页查询会话

- **URL**：`GET /ai/conversation/page`

#### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 否 | 用户 ID |
| userType | String | 否 | 用户类型 |
| keyword | String | 否 | 标题关键字 |
| status | Integer | 否 | 状态码 |
| pageNum | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 页大小，默认 10 |

#### 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  }
}
```

### 2.2 查询会话消息

- **URL**：`GET /ai/conversation/{id}/messages`

#### 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "role": "user",
      "content": "你好",
      "createTime": "2026-08-20 10:00:00"
    }
  ]
}
```

### 2.3 归档会话

- **URL**：`PUT /ai/conversation/{id}/archive`

### 2.4 删除会话

- **URL**：`DELETE /ai/conversation/{id}`

---

## 3. 模型提供者管理接口

基础路径：`/ai/provider`

### 3.1 查询所有提供者

- **URL**：`GET /ai/provider/list`

### 3.2 新增 / 修改提供者

- **URL**：`POST /ai/provider`

#### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 否 | 为空新增，非空修改 |
| name | String | 是 | 名称 |
| code | String | 是 | 唯一编码 |
| type | String | 是 | `OPENAI_COMPATIBLE` / `DASHSCOPE` |
| endpoint | String | 否 | API 端点 |
| apiKey | String | 否 | API Key |
| model | String | 否 | 默认模型 |
| priority | Integer | 否 | 优先级 |
| enabled | Integer | 否 | 0-禁用 1-启用 |
| configJson | String | 否 | 扩展配置 JSON |

### 3.3 切换启用状态

- **URL**：`PUT /ai/provider/{id}/toggle`

---

## 4. 知识库管理接口

基础路径：`/ai/knowledge`

### 4.1 新增 / 修改知识库

- **URL**：`POST /ai/knowledge/base`

#### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 否 | 为空新增 |
| name | String | 是 | 名称 |
| description | String | 否 | 描述 |
| embeddingModel | String | 否 | 向量模型 |
| providerCode | String | 否 | 向量服务提供者编码 |
| status | Integer | 否 | 0-正常 1-禁用 |

### 4.2 分页查询知识库

- **URL**：`GET /ai/knowledge/base/page`

### 4.3 新增文档

- **URL**：`POST /ai/knowledge/document`

#### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| kbId | Long | 是 | 知识库 ID |
| title | String | 否 | 文档标题 |
| sourceType | String | 否 | `TEXT` / `FILE` / `URL` |
| sourceUrl | String | 否 | 来源地址 |
| content | String | 是 | 文档内容 |

### 4.4 删除文档

- **URL**：`DELETE /ai/knowledge/document/{id}`

### 4.5 向量检索

- **URL**：`GET /ai/knowledge/retrieve`

#### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| kbId | Long | 是 | 知识库 ID |
| query | String | 是 | 查询文本 |
| topK | int | 否 | 返回条数，默认 5 |

#### 响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    "片段 1 内容...",
    "片段 2 内容..."
  ]
}
```

---

## 5. 公共枚举

### 5.1 ConversationStatus

| code | 含义 |
|------|------|
| 0 | 正常 |
| 1 | 归档 |
| 2 | 删除 |

### 5.2 MessageRole

| code | 含义 |
|------|------|
| user | 用户 |
| assistant | 助手 |
| system | 系统 |
| tool | 工具 |

### 5.3 ProviderType

| code | 含义 |
|------|------|
| OPENAI_COMPATIBLE | OpenAI 兼容接口 |
| DASHSCOPE | 阿里云百炼 |
