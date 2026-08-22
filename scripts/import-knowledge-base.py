#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量导入知识库文档到 xy-ai 系统。

前置条件：
1. xy-admin（端口 9090）和 xy-ai（端口 9070）已启动。
2. 数据库中已存在可登录的管理员账号（默认 admin/admin123 或按实际情况修改）。
3. ai_model_provider 表中已配置好 Embedding 提供者（如 dashscope），或 .env 中已配置 EMBEDDING_*。

用法：
    python scripts/import-knowledge-base.py

默认读取 docs/knowledge-base/ 下的 .md 文件，每个文件创建一个知识库并上传为文档。
"""

import argparse
import os
import sys
from pathlib import Path

import requests

# 默认配置
DEFAULT_ADMIN_BASE_URL = "http://localhost:9090"
DEFAULT_AI_BASE_URL = "http://localhost:9070"
DEFAULT_USERNAME = "admin"
DEFAULT_PASSWORD = "admin123"
DEFAULT_KB_DIR = Path(__file__).resolve().parent.parent / "docs" / "knowledge-base"

# 知识库元数据：文件名 -> (知识库名称, 描述, 是否独立 Embedding 配置)
# provider_code / embedding_model 为空时，走全局 ai.embedding.* 配置
KNOWLEDGE_BASES = [
    {
        "file": "01-project-overview.md",
        "name": "项目整体概览",
        "description": "xy-service 项目架构、模块划分、技术栈与工程约定",
        "provider_code": "",
        "embedding_model": "",
    },
    {
        "file": "02-ai-module-architecture.md",
        "name": "AI 模块架构与 RAG",
        "description": "xy-ai 模块架构、RAG 流程、双轨 Embedding 配置与常见错误排查",
        "provider_code": "",
        "embedding_model": "",
    },
    {
        "file": "03-database-schema.md",
        "name": "数据库表结构",
        "description": "xy-service 各模块数据库表结构说明",
        "provider_code": "",
        "embedding_model": "",
    },
    {
        "file": "04-authentication-authorization.md",
        "name": "认证授权与 API Key",
        "description": "JWT 认证、匿名白名单、API Key 机制",
        "provider_code": "",
        "embedding_model": "",
    },
    {
        "file": "05-deployment-guide.md",
        "name": "部署与运行指南",
        "description": "环境准备、配置、启动顺序与常见问题",
        "provider_code": "",
        "embedding_model": "",
    },
]


def login(admin_base_url: str, username: str, password: str) -> str:
    """管理员登录，返回 JWT token。"""
    url = f"{admin_base_url}/auth/login"
    payload = {"username": username, "password": password}
    resp = requests.post(url, json=payload, timeout=30)
    resp.raise_for_status()
    data = resp.json()
    if data.get("code") != 200:
        raise RuntimeError(f"登录失败: {data}")
    token = data.get("data", {}).get("token")
    if not token:
        raise RuntimeError("登录响应中未找到 token")
    print(f"[OK] 管理员 {username} 登录成功")
    return token


def create_knowledge_base(ai_base_url: str, token: str, meta: dict) -> int:
    """创建知识库，返回知识库 ID。"""
    url = f"{ai_base_url}/ai/knowledge/base"
    headers = {"Authorization": f"Bearer {token}"}
    payload = {
        "name": meta["name"],
        "description": meta["description"],
        "providerCode": meta.get("provider_code", ""),
        "embeddingModel": meta.get("embedding_model", ""),
    }
    resp = requests.post(url, json=payload, headers=headers, timeout=30)
    resp.raise_for_status()
    data = resp.json()
    if data.get("code") != 200:
        raise RuntimeError(f"创建知识库失败 [{meta['name']}]: {data}")

    # 查询刚创建的知识库 ID
    page_resp = requests.get(
        f"{ai_base_url}/ai/knowledge/base/page",
        headers=headers,
        params={"keyword": meta["name"], "pageNum": 1, "pageSize": 10},
        timeout=30,
    )
    page_resp.raise_for_status()
    page_data = page_resp.json()
    if page_data.get("code") != 200:
        raise RuntimeError(f"查询知识库失败 [{meta['name']}]: {page_data}")

    rows = page_data.get("data", {}).get("rows", [])
    for row in rows:
        if row.get("name") == meta["name"]:
            kb_id = row.get("id")
            print(f"[OK] 创建知识库 [{meta['name']}]，ID={kb_id}")
            return kb_id
    raise RuntimeError(f"无法找到刚创建的知识库 [{meta['name']}]")


def add_document(ai_base_url: str, token: str, kb_id: int, title: str, content: str):
    """向知识库添加文档。"""
    url = f"{ai_base_url}/ai/knowledge/document"
    headers = {"Authorization": f"Bearer {token}"}
    payload = {
        "kbId": kb_id,
        "title": title,
        "sourceType": "markdown",
        "content": content,
    }
    resp = requests.post(url, json=payload, headers=headers, timeout=60)
    resp.raise_for_status()
    data = resp.json()
    if data.get("code") != 200:
        raise RuntimeError(f"添加文档失败 [{title}]: {data}")
    print(f"[OK] 添加文档 [{title}]，长度={len(content)} 字符")


def main():
    parser = argparse.ArgumentParser(description="批量导入 xy-service 知识库文档")
    parser.add_argument("--admin-url", default=DEFAULT_ADMIN_BASE_URL, help="xy-admin 基础地址")
    parser.add_argument("--ai-url", default=DEFAULT_AI_BASE_URL, help="xy-ai 基础地址")
    parser.add_argument("--username", default=DEFAULT_USERNAME, help="管理员用户名")
    parser.add_argument("--password", default=DEFAULT_PASSWORD, help="管理员密码")
    parser.add_argument("--kb-dir", default=str(DEFAULT_KB_DIR), help="知识库文档目录")
    parser.add_argument(
        "--provider-code",
        default="",
        help="统一指定 Embedding 提供者编码（会覆盖每个知识库的独立配置）",
    )
    parser.add_argument(
        "--embedding-model",
        default="",
        help="统一指定 Embedding 模型（会覆盖每个知识库的独立配置）",
    )
    args = parser.parse_args()

    kb_dir = Path(args.kb_dir)
    if not kb_dir.exists():
        print(f"[ERROR] 知识库文档目录不存在: {kb_dir}")
        sys.exit(1)

    token = login(args.admin_url, args.username, args.password)

    for meta in KNOWLEDGE_BASES:
        file_path = kb_dir / meta["file"]
        if not file_path.exists():
            print(f"[WARN] 文件不存在，跳过: {file_path}")
            continue

        content = file_path.read_text(encoding="utf-8")

        # 应用命令行统一覆盖
        if args.provider_code:
            meta["provider_code"] = args.provider_code
        if args.embedding_model:
            meta["embedding_model"] = args.embedding_model

        kb_id = create_knowledge_base(args.ai_url, token, meta)
        add_document(args.ai_url, token, kb_id, meta["name"], content)

    print("\n[OK] 全部知识库导入完成")


if __name__ == "__main__":
    main()
