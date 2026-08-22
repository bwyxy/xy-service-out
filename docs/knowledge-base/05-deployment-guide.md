# xy-service 部署与运行指南

## 环境准备

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 6+
- Node.js 20+ / pnpm

## 配置文件

所有模块共享项目根目录的 .env 文件，模板见 .env.example。

关键配置项：

```properties
# MySQL
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=xy_service
MYSQL_USERNAME=root
MYSQL_PASSWORD=

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
TOKEN_SECRET=your-secret

# 聊天用 DeepSeek
OPENAI_API_KEY=sk-xxx
OPENAI_BASE_URL=https://api.deepseek.com
OPENAI_MODEL=deepseek-v4-flash

# Embedding 用阿里云 DashScope
EMBEDDING_API_KEY=sk-xxx
EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
EMBEDDING_MODEL=text-embedding-v3

# 七牛云
QINIU_ACCESS_KEY=
QINIU_SECRET_KEY=
QINIU_BUCKET=
QINIU_DOMAIN=

# 悟空 IM
WUKONGIM_API_URL=
WUKONGIM_MANAGER_TOKEN=
```

## 启动后端

按依赖顺序启动：

1. xy-system / xy-admin（端口 9090）
2. common-api/xy-ai（端口 9070）
3. customer-api/fg-chat（端口 9080）
4. customer-api/xy-app（端口 9100）
5. customer-api/xy-blog

示例：

```bash
cd xy-system
mvn spring-boot:run

cd ../xy-admin
mvn spring-boot:run

cd ../common-api/xy-ai
mvn spring-boot:run
```

## 启动前端

```bash
cd y-micro-app
pnpm install
pnpm dev
```

## 数据库初始化

按顺序执行 sql 目录下的脚本：

1. sql/todo.sql
2. sql/ai.sql
3. sql/chat.sql
4. sql/blog.sql

## 知识库导入

知识库文档必须通过 API 导入，才能自动分片和生成向量。直接 SQL 插入的文档状态为"处理中"，无向量数据，无法用于 RAG 检索。

## 常见问题

- 模块启动失败先检查 .env 是否加载、MySQL/Redis 是否可达。
- AI 对话返回 401 检查 API Key 或 JWT token。
- RAG 返回 404 检查 Embedding 配置是否指向支持 Embedding 的服务。
