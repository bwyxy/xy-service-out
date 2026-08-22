# xy-service 认证授权与 API Key 设计

## JWT 认证

- 入口过滤器：com.chat.ai.security.JwtAuthenticationFilter
- JWT 工具：com.xy.common.utils.JwtUtils
- 配置：
  - jwt.header=accessToken
  - jwt.secret=${TOKEN_SECRET}
  - jwt.expiration=86400000
  - jwt.refresh-expiration=604800000

## 匿名白名单

- 注解：@Anonymous
- 配置类：PermitAllUrlProperties
- 用于开放接口，如 /ai/auth/exchange

## API Key 机制

### 用途

外部应用通过固定 API Key 换取临时 JWT token，再访问 AI 对话接口。

### 核心流程

1. 客户端携带 API Key 调用 /ai/auth/exchange
2. 服务端校验 API Key 是否有效
3. 校验通过生成临时 JWT token 返回
4. 客户端用 JWT token 调用 /ai/chat

### 核心类

- 换 token 接口：com.chat.ai.controller.AiAuthController
- 校验与签发：com.xy.system.service.impl.SysAiApiKeyServiceImpl
- 管理后台：com.xy.admin.controller.AiApiKeyController

### 安全处理

- API Key Secret 使用 BCrypt 加密存储
- 数据库中 sys_ai_api_key.secret 字段存储密文
- 校验时使用 BCryptPasswordEncoder.matches

## 跨模块调用

- xy-ai 通过 OpenFeign 调用 xy-system 的 SysAiApiKeyService
- Feign Client 位于 xy-common 的 com.xy.common.feign 包
