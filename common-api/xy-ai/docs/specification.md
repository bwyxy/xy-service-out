# AI 模块开发规范

## 1. 代码结构规范

### 1.1 包结构

```
com.chat.ai
├── config          # 配置类
├── controller      # REST 控制器
├── dto             # 请求/响应数据传输对象
├── entity          # MyBatis-Plus 实体
├── enums           # 枚举
├── exception       # 自定义异常
├── mapper          # MyBatis Mapper 接口
├── provider
│   ├── core        # 模型提供者抽象与工厂
│   ├── openai      # OpenAI 兼容实现
│   └── dashscope   # 阿里云百炼实现
├── rag             # RAG 相关组件
├── service         # 服务接口与实现
├── tool            # Agent 工具
│   ├── core        # 工具框架
│   └── impl        # 工具实现
├── vo              # 视图对象
└── XyAiApplication.java
```

### 1.2 命名规范

| 类型 | 命名 | 示例 |
|------|------|------|
| 控制器 | XxxController | `AiController` |
| 服务接口 | AiXxxService | `AiChatService` |
| 服务实现 | AiXxxServiceImpl | `AiChatServiceImpl` |
| Mapper | XxxMapper | `AiConversationMapper` |
| 实体 | AiXxx | `AiConversation` |
| DTO | XxxRequest / XxxResponse / XxxQuery | `ChatRequest` |
| VO | XxxVo | `ConversationVo` |
| 枚举 | XxxStatus / XxxType / XxxRole | `ConversationStatus` |

## 2. 编码规范

### 2.1 使用 Lombok

实体、DTO、VO 使用 Lombok 减少样板代码：

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse { ... }
```

日志使用 `@Slf4j`。

### 2.2 异常处理

- 业务异常统一使用 `AiException`。
- 异常信息需明确、可被前端展示。
- 不要在业务层吞掉异常后返回 null，应抛出异常或返回明确的错误对象。

### 2.3 事务控制

Service 层涉及多张表写操作时，需在实现类或方法上加 `@Transactional`。

### 2.4 返回值

- Controller 统一返回 `R<T>`。
- 分页统一返回 `PageResult<T>`。
- 列表查询返回 `List<T>` 后由 `R.ok(...)` 包装。

## 3. 数据库规范

### 3.1 表命名

统一前缀 `ai_`，全部小写，下划线分隔：

- `ai_conversation`
- `ai_knowledge_document`

### 3.2 字段命名

- 主键：`id BIGINT PRIMARY KEY AUTO_INCREMENT`
- 状态字段：`status TINYINT`，默认 0
- 时间字段：`create_time` / `update_time DATETIME`
- JSON 字段：以 `_json` 结尾，Java 实体中用 `String` 存储

### 3.3 索引

- 外键与查询条件必须建立索引。
- 唯一约束使用 `UNIQUE INDEX uk_xxx`。

### 3.4 SQL 脚本

- 建表语句统一放在 `sql/ai.sql`。
- 测试 Schema 统一放在 `src/test/resources/schema-test.sql`，并保持与生产表结构一致。

## 4. 模型提供者开发规范

### 4.1 新增提供者步骤

1. 在 `ProviderType` 枚举新增类型。
2. 创建 `com.chat.ai.provider.xxx.XxxModelProvider` 实现 `ModelProvider`。
3. 在 `ModelProviderFactory#buildProvider` 补充创建逻辑。
4. 单元测试覆盖 `chat` 与 `streamChat`。

### 4.2 工具注册

```java
@Override
public void registerTool(String name, String description, String schema, Function<String, String> executor) {
    FunctionToolCallback.Builder<String, String> builder = FunctionToolCallback.builder(name, executor);
    builder.description(description);
    builder.inputSchema(schema);
    builder.inputType(String.class);
    toolCallbacks.put(name, builder.build());
}
```

## 5. Agent 工具开发规范

### 5.1 接口实现

工具类必须实现 `ToolExecutor` 并注册为 Spring Bean：

```java
@Component("weatherQueryTool")
public class WeatherQueryTool implements ToolExecutor {
    @Override
    public String execute(String argumentsJson) { ... }
}
```

### 5.2 参数解析

使用 Jackson 解析入参 JSON，避免手动字符串拼接。

### 5.3 安全约束

- 工具执行前校验参数。
- 禁止直接执行用户传入的脚本或命令。
- 异常时返回可读错误信息，不要抛出未捕获异常。

## 6. 测试规范

### 6.1 测试配置

- 使用 `@ActiveProfiles("test")`。
- 测试数据库使用 H2，Schema 由 `schema-test.sql` 初始化。
- 敏感配置（jwt、redis）在 `application-test.yml` 中提供 mock 值。

### 6.2 测试覆盖

- Service 核心逻辑必须编写单元 / 集成测试。
- 工具类必须编写独立单元测试。
- 新增模型提供者需补充 Mock 测试。

### 6.3 提交前检查

```bash
mvn clean test -pl chat-api/xy-ai -am
```

## 7. 日志与监控

- 使用 SLF4J + `@Slf4j`。
- 关键路径打印 INFO 级别日志：对话请求、模型选择、工具调用、RAG 检索。
- 异常打印 WARN / ERROR 级别日志。
- 流式输出异常必须捕获并返回 SSE 错误事件，避免连接挂起。
