# AI 模块 API Key 管理技术文档

## 1. 设计目标

为 AI 服务提供安全、可追溯、可管理的 API Key 访问控制能力，核心逻辑与数据层下沉至 xy-system，xy-ai 仅保留 token 换取能力，支持：

- 高熵随机密钥生成
- Secret 安全存储（BCrypt 哈希）
- 密钥生命周期管理（启用 / 禁用 / 删除）
- 后台管理端统一生成入口
- 与现有 JWT 认证体系集成

## 2. 生成规则

### 2.1 API Key

- **格式**：固定前缀 `xyai_` + 32 字符随机 Base62 字符串
- **长度**：36 字符
- **字符集**：`A-Z`、`a-z`、`0-9`，共 62 个字符
- **熵值**：约 190 bit，暴力破解不可行
- **示例**：`xyai_aK7mPq9vR2xYz3wBc4De5Fg6HiJk8LmN`

### 2.2 Secret Key

- **格式**：64 字节随机二进制，经 Base64 编码后约 88 字符
- **字符集**：Base64 字符集（含 `+`、`/`、`=`）
- **熵值**：512 bit
- **存储**：数据库存储 BCrypt 哈希，不存明文
- **示例**：`x9B2cD4eF6gH8iJ0kL1mN3oP5qR7sT9uV0wX2yZ4aB6cD8eF0gH2iJ4kL6mN8oP0qR2sT4uV6wX8yZ0aB2cD4=`

> Secret 仅在创建时通过接口返回一次，丢失后无法找回，需重新生成。

## 3. 生成算法

使用 `java.security.SecureRandom` 生成密码学安全随机数：

1. API Key：生成 24 字节随机数 → Base62 编码为 32 字符 → 拼接前缀 `xyai_`
2. Secret：生成 64 字节随机数 → Base64 编码
3. 使用 BCrypt 对 Secret 进行哈希，哈希结果存入数据库

## 4. 数据表

```sql
CREATE TABLE ai_api_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_name VARCHAR(64) NOT NULL,
    api_key VARCHAR(64) NOT NULL,
    api_secret VARCHAR(255) NOT NULL,
    enabled TINYINT DEFAULT 1,
    expire_time DATETIME,
    token_expire_seconds INT DEFAULT 3600,
    create_time DATETIME,
    update_time DATETIME,
    UNIQUE INDEX uk_api_key (api_key)
);
```

## 5. 核心接口

### 5.1 xy-ai 原生接口

| 接口 | 说明 |
|------|------|
| `POST /ai/auth/token` | API Key + Secret 换取 JWT |

### 5.2 xy-admin 管理接口

| 接口 | 说明 |
|------|------|
| `POST /ai/api-key/generate` | 生成密钥对 |
| `POST /ai/api-key/page` | 分页查询 |
| `PUT /ai/api-key/{id}/enable` | 启用 |
| `PUT /ai/api-key/{id}/disable` | 禁用 |
| `DELETE /ai/api-key/{id}` | 删除 |

xy-admin 直接调用 xy-system 的 `ISysAiApiKeyService`，管理接口与 AI 服务解耦，但共用同一张 `ai_api_key` 数据表。

## 6. 验证机制

1. 根据 `api_key` 查询数据库
2. 检查密钥是否存在、已启用、未过期
3. 使用 `PasswordEncoder` 比对 BCrypt 哈希后的 `api_secret`
4. 校验通过后，使用 JWT 私钥签发 token，subject 为 API Key

## 7. 后台管理接入

### 7.1 调用流程

```
管理员浏览器 → xy-admin:9090/ai/api-key/generate
                 ↓
         AiApiKeyController (xy-admin)
                 ↓
         ISysAiApiKeyService (xy-system)
                 ↓
         SysAiApiKeyMapper → ai_api_key
                 ↓
         返回 API Key + Secret（仅一次）
```

AI 服务侧仅保留 `/ai/auth/token`，用于将 API Key + Secret 换取访问令牌：

```
调用方 → xy-ai:9070/ai/auth/token
                 ↓
         AiAuthController (xy-ai)
                 ↓
         ISysAiApiKeyService.exchangeToken (xy-system)
                 ↓
         返回 JWT
```

### 7.3 前端页面

前端管理页面位于：

- [src/views/ai/apiKey/index.vue](file:///Users/xiaoyi/me-project/todo-service/y-micro-app/apps/y-admin-sub/src/views/ai/apiKey/index.vue)
- [src/api/ai/apiKey.ts](file:///Users/xiaoyi/me-project/todo-service/y-micro-app/apps/y-admin-sub/src/api/ai/apiKey.ts)

页面功能：

- 分页展示 API Key 列表
- 按应用名称、API Key、状态搜索
- 生成新密钥对并弹窗展示 Secret（仅一次）
- 一键复制 API Key / Secret
- 启用 / 禁用 / 删除密钥

菜单组件路径配置为 `ai/apiKey/index`，权限字符为：

- `ai:apiKey:list` — 查看列表
- `ai:apiKey:add` — 生成密钥
- `ai:apiKey:edit` — 启用/禁用
- `ai:apiKey:remove` — 删除

## 8. 安全配置

- 生产环境务必通过 HTTPS 暴露相关接口
- 删除示例密钥 `demo-api-key`
- 定期轮换密钥
- 对异常调用进行限流和审计

## 9. 测试

- `ApiKeyGeneratorTest`：验证格式、熵值、唯一性
- `SysAiApiKeyServiceImplTest`：验证生成、启用/禁用/删除、换 token 流程

运行：

```bash
mvn clean test -pl xy-system -am
mvn clean test -pl chat-api/xy-ai -am
```
