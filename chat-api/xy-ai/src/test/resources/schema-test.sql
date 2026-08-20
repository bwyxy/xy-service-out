-- H2 测试用表结构

CREATE TABLE IF NOT EXISTS ai_model_provider (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    endpoint VARCHAR(255) DEFAULT '',
    api_key VARCHAR(255) DEFAULT '',
    model VARCHAR(64) DEFAULT '',
    priority INT DEFAULT 0,
    enabled TINYINT DEFAULT 1,
    config_json VARCHAR(1000),
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_type VARCHAR(16) DEFAULT 'CLIENT',
    title VARCHAR(128) DEFAULT '',
    provider_code VARCHAR(64) DEFAULT '',
    model VARCHAR(64) DEFAULT '',
    system_prompt TEXT,
    params_json VARCHAR(1000),
    status TINYINT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT,
    tool_calls_json VARCHAR(2000),
    tool_call_id VARCHAR(64) DEFAULT '',
    usage_json VARCHAR(1000),
    model VARCHAR(64) DEFAULT '',
    status TINYINT DEFAULT 0,
    error_msg VARCHAR(500) DEFAULT '',
    create_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(500) DEFAULT '',
    embedding_model VARCHAR(64) DEFAULT '',
    provider_code VARCHAR(64) DEFAULT '',
    status TINYINT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id BIGINT NOT NULL,
    title VARCHAR(255) DEFAULT '',
    source_type VARCHAR(32) DEFAULT 'TEXT',
    source_url VARCHAR(500) DEFAULT '',
    content TEXT,
    chunk_count INT DEFAULT 0,
    status TINYINT DEFAULT 0,
    error_msg VARCHAR(500) DEFAULT '',
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id BIGINT NOT NULL,
    kb_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding_json VARCHAR(4000),
    metadata_json VARCHAR(2000),
    vector_id VARCHAR(64) DEFAULT '',
    create_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_tool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(500) DEFAULT '',
    parameters_schema VARCHAR(2000),
    implementation_bean VARCHAR(128) DEFAULT '',
    enabled TINYINT DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_user_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_type VARCHAR(16) DEFAULT 'CLIENT',
    daily_limit INT DEFAULT 100,
    daily_used INT DEFAULT 0,
    total_used BIGINT DEFAULT 0,
    reset_time TIMESTAMP,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);
