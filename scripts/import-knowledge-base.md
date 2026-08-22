# 知识库批量导入脚本

脚本位置：[import-knowledge-base.py](import-knowledge-base.py)

## 前置条件

1. `xy-admin`（端口 9090）和 `xy-ai`（端口 9070）已启动。
2. 数据库中已存在可登录的管理员账号。
3. 已配置好 Embedding：
   - 方式 A：`.env` 中配置 `EMBEDDING_*` 全局变量（推荐）
   - 方式 B：`ai_model_provider` 表中已有可用的 Embedding 提供者（如 `dashscope`）

## 安装依赖

```bash
pip install requests
```

## 默认用法

```bash
python scripts/import-knowledge-base.py
```

默认使用：

- 管理员账号：`admin / admin123`
- `xy-admin`：`http://localhost:9090`
- `xy-ai`：`http://localhost:9070`
- 文档目录：`docs/knowledge-base/`

## 自定义参数

```bash
python scripts/import-knowledge-base.py \
  --admin-url http://localhost:9090 \
  --ai-url http://localhost:9070 \
  --username admin \
  --password your-password \
  --provider-code dashscope \
  --embedding-model text-embedding-v3
```

参数说明：

| 参数 | 说明 |
|---|---|
| `--admin-url` | xy-admin 基础地址 |
| `--ai-url` | xy-ai 基础地址 |
| `--username` | 管理员用户名 |
| `--password` | 管理员密码 |
| `--kb-dir` | 知识库文档目录 |
| `--provider-code` | 统一指定 Embedding 提供者编码，覆盖每个知识库的独立配置 |
| `--embedding-model` | 统一指定 Embedding 模型，覆盖每个知识库的独立配置 |

## 导入内容

默认导入 `docs/knowledge-base/` 下的 5 个文档：

1. 项目整体概览
2. AI 模块架构与 RAG
3. 数据库表结构
4. 认证授权与 API Key
5. 部署与运行指南

## 导入后验证

调用检索接口测试：

```bash
curl -G 'http://localhost:9070/ai/knowledge/retrieve' \
  --data-urlencode 'kbId=1' \
  --data-urlencode 'query=xy-service 有哪些模块' \
  -H 'Authorization: Bearer 你的token'
```

如果返回相关片段，说明知识库已成功分片并生成向量。
