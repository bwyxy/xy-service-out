# AI 模块测试报告

## 1. 测试概述

| 项目 | 内容 |
|------|------|
| 模块 | chat-api/xy-ai |
| 测试类型 | 单元测试 + Spring Boot 集成测试 |
| 测试框架 | JUnit 5、Spring Boot Test、H2 |
| 测试时间 | 2026-08-20 |
| 执行命令 | `mvn clean test -pl chat-api/xy-ai -am` |

## 2. 测试结果

```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| 测试类 | 用例数 | 通过 | 失败 | 错误 |
|--------|--------|------|------|------|
| AiConversationServiceImplTest | 4 | 4 | 0 | 0 |
| AiKnowledgeBaseServiceImplTest | 2 | 2 | 0 | 0 |
| AiProviderServiceImplTest | 4 | 4 | 0 | 0 |
| AiQuotaServiceImplTest | 2 | 2 | 0 | 0 |
| ToolImplTest | 3 | 3 | 0 | 0 |
| **合计** | **15** | **15** | **0** | **0** |

## 3. 测试覆盖范围

### 3.1 会话服务测试（AiConversationServiceImplTest）

- 创建会话并保存用户消息
- 分页查询会话列表
- 查询会话消息列表
- 更新会话状态（归档 / 删除）

### 3.2 知识库服务测试（AiKnowledgeBaseServiceImplTest）

- 创建知识库
- 添加文档并检索片段

### 3.3 模型提供者服务测试（AiProviderServiceImplTest）

- 插入默认提供者后返回默认提供者
- 按优先级排序列出提供者
- 切换提供者启用状态
- 无可用提供者时抛出异常

### 3.4 配额服务测试（AiQuotaServiceImplTest）

- 首次消费时初始化配额
- 超过每日上限时抛出异常

### 3.5 工具测试（ToolImplTest）

- `CurrentTimeTool` 返回格式化时间
- `CalculatorTool` 正确计算 `1+2*3`
- `CalculatorTool` 拒绝包含非法字符的表达式

## 4. 测试环境

### 4.1 数据库

- H2 内存数据库
- 模式初始化：`src/test/resources/schema-test.sql`
- 兼容 MySQL 语法：`MODE=MySQL`

### 4.2 外部依赖 Mock

- Redis：配置为本地，测试中未实际触发。
- Embedding 模型：使用 `EmptyEmbeddingModel` 兜底。
- JWT：在 `application-test.yml` 中提供测试密钥。

### 4.3 关键配置

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:xy_ai_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
  sql:
    init:
      schema-locations: classpath:schema-test.sql
      mode: always
```

## 5. 问题与修复记录

| 问题 | 原因 | 修复方式 |
|------|------|----------|
| Lombok 注解未解析 | xy-ai 模块未声明 lombok 依赖 | 在 `pom.xml` 中添加 `lombok` 依赖 |
| `EmbeddingService` 返回类型不匹配 | Spring AI 1.1.0 中 `Embedding.getOutput()` 返回 `float[]` | 将 `float[]` 转换为 `List<Double>` |
| `EmptyEmbeddingModel` 编译错误 | 未实现 `embed(Document)`，且 `Embedding` 构造函数参数变更 | 实现缺失方法，使用 `new Embedding(float[], Integer)` |
| `AiException` 构造函数报错 | `ServiceException` 无 `(String, Throwable)` 构造函数 | 改为调用 `super(message)` 并 `initCause(cause)` |
| 测试上下文加载失败 | H2 表未初始化 | 配置 `spring.sql.init.schema-locations` |
| JWT 占位符无法解析 | 缺少 `jwt.*` 配置 | 在 `application-test.yml` 中补充测试配置 |
| 缺少 `PasswordEncoder` Bean | xy-ai 未依赖 xy-framework | 在 xy-ai 中独立提供 `PasswordEncoderConfig` |
| `CalculatorTool` 计算失败 | Java 17 移除了 Nashorn 脚本引擎 | 使用双栈法自研表达式求值 |

## 6. 质量验收标准

| 验收项 | 标准 | 结果 |
|--------|------|------|
| 编译通过 | `mvn clean compile` 无错误 | 通过 |
| 单元测试通过 | 测试成功率 100% | 通过（15/15） |
| 无严重代码异味 | 无明显空指针、资源泄露风险 | 通过 |
| 文档完整 | 架构/API/指南/规范/测试报告齐全 | 通过 |
| 可独立构建 | `mvn clean test -pl chat-api/xy-ai -am` 成功 | 通过 |

## 7. 后续建议

1. 补充模型提供者的 Mock 集成测试，覆盖流式输出路径。
2. 补充 RAG 端到端测试，验证文档分片、向量检索、上下文注入流程。
3. 补充 Agent 工具调用的集成测试。
4. 生产环境部署前接入真实 Embedding 模型或专用向量数据库。
