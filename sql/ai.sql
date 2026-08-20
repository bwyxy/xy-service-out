-- ==================== AI 模块数据表 ====================

-- 模型提供者配置表
CREATE TABLE ai_model_provider (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    name VARCHAR(64) NOT NULL COMMENT '提供者名称，如 OpenAI、阿里云百炼',
    code VARCHAR(64) NOT NULL COMMENT '唯一编码，如 openai、dashscope',
    type VARCHAR(32) NOT NULL COMMENT '类型：OPENAI_COMPATIBLE / DASHSCOPE / QIANFAN',
    endpoint VARCHAR(255) DEFAULT '' COMMENT 'API 端点地址',
    api_key VARCHAR(255) DEFAULT '' COMMENT 'API Key',
    model VARCHAR(64) DEFAULT '' COMMENT '默认模型名称',
    priority INT DEFAULT 0 COMMENT '优先级，数值越大越优先',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用 0-禁用 1-启用',
    config_json JSON COMMENT '扩展配置 JSON',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    UNIQUE INDEX uk_code (code),
    INDEX idx_enabled_priority (enabled, priority)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 模型提供者配置表';

-- AI 会话表
CREATE TABLE ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    user_type VARCHAR(16) DEFAULT 'CLIENT' COMMENT '用户类型：CLIENT 客户端用户 / ADMIN 管理员',
    title VARCHAR(128) DEFAULT '' COMMENT '会话标题',
    provider_code VARCHAR(64) DEFAULT '' COMMENT '使用的模型提供者编码',
    model VARCHAR(64) DEFAULT '' COMMENT '使用的模型',
    system_prompt TEXT COMMENT '系统提示词',
    params_json JSON COMMENT '生成参数，如 temperature、maxTokens',
    status TINYINT DEFAULT 0 COMMENT '状态 0-正常 1-归档 2-删除',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_user (user_id, user_type, status),
    INDEX idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 会话表';

-- AI 消息表
CREATE TABLE ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息 ID',
    conversation_id BIGINT NOT NULL COMMENT '所属会话 ID',
    role VARCHAR(16) NOT NULL COMMENT '角色：user / assistant / system / tool',
    content TEXT COMMENT '消息内容',
    tool_calls_json JSON COMMENT '模型请求调用的工具列表',
    tool_call_id VARCHAR(64) DEFAULT '' COMMENT '关联的工具调用 ID',
    usage_json JSON COMMENT 'Token 消耗统计',
    model VARCHAR(64) DEFAULT '' COMMENT '实际使用的模型',
    status TINYINT DEFAULT 0 COMMENT '状态 0-正常 1-生成中 2-失败',
    error_msg VARCHAR(500) DEFAULT '' COMMENT '错误信息',
    create_time DATETIME COMMENT '创建时间',
    INDEX idx_conversation (conversation_id, create_time),
    INDEX idx_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 消息表';

-- 知识库表
CREATE TABLE ai_knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识库 ID',
    name VARCHAR(128) NOT NULL COMMENT '知识库名称',
    description VARCHAR(500) DEFAULT '' COMMENT '知识库描述',
    embedding_model VARCHAR(64) DEFAULT '' COMMENT '向量模型',
    provider_code VARCHAR(64) DEFAULT '' COMMENT '向量服务提供者编码',
    status TINYINT DEFAULT 0 COMMENT '状态 0-正常 1-禁用',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 知识库表';

-- 知识库文档表
CREATE TABLE ai_knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档 ID',
    kb_id BIGINT NOT NULL COMMENT '所属知识库 ID',
    title VARCHAR(255) DEFAULT '' COMMENT '文档标题',
    source_type VARCHAR(32) DEFAULT 'TEXT' COMMENT '来源类型：TEXT / FILE / URL',
    source_url VARCHAR(500) DEFAULT '' COMMENT '文件或 URL 地址',
    content TEXT COMMENT '文档原始内容',
    chunk_count INT DEFAULT 0 COMMENT '分片数量',
    status TINYINT DEFAULT 0 COMMENT '状态 0-处理中 1-已完成 2-失败',
    error_msg VARCHAR(500) DEFAULT '' COMMENT '错误信息',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_kb (kb_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 知识库文档表';

-- 知识库文档片段表（用于本地向量检索兜底，生产环境建议接入专用向量数据库）
CREATE TABLE ai_knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '片段 ID',
    doc_id BIGINT NOT NULL COMMENT '所属文档 ID',
    kb_id BIGINT NOT NULL COMMENT '所属知识库 ID',
    content TEXT NOT NULL COMMENT '片段内容',
    embedding_json JSON COMMENT '向量嵌入 JSON 数组',
    metadata_json JSON COMMENT '元数据',
    vector_id VARCHAR(64) DEFAULT '' COMMENT '外部向量库 ID',
    create_time DATETIME COMMENT '创建时间',
    INDEX idx_kb_doc (kb_id, doc_id),
    INDEX idx_vector (vector_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 知识库文档片段表';

-- Agent 工具注册表
CREATE TABLE ai_tool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工具 ID',
    name VARCHAR(64) NOT NULL COMMENT '工具名称',
    code VARCHAR(64) NOT NULL COMMENT '唯一编码',
    description VARCHAR(500) DEFAULT '' COMMENT '工具描述',
    parameters_schema JSON COMMENT '参数 JSON Schema',
    implementation_bean VARCHAR(128) DEFAULT '' COMMENT 'Spring Bean 名称',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用 0-禁用 1-启用',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    UNIQUE INDEX uk_code (code),
    INDEX idx_enabled (enabled)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI Agent 工具注册表';

-- 用户配额表
CREATE TABLE ai_user_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配额 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    user_type VARCHAR(16) DEFAULT 'CLIENT' COMMENT '用户类型',
    daily_limit INT DEFAULT 100 COMMENT '每日调用上限',
    daily_used INT DEFAULT 0 COMMENT '今日已用次数',
    total_used BIGINT DEFAULT 0 COMMENT '累计使用次数',
    reset_time DATETIME COMMENT '每日重置时间',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    UNIQUE INDEX uk_user (user_id, user_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 用户调用配额表';

-- 初始化默认模型提供者（OpenAI 兼容接口）
INSERT INTO ai_model_provider (name, code, type, endpoint, api_key, model, priority, enabled, config_json, create_time, update_time) VALUES
('OpenAI 兼容接口', 'openai', 'OPENAI_COMPATIBLE', 'https://ws-cn7y90ayoe81zjvl.cn-beijing.maas.aliyuncs.com/compatible-mode', '', 'deepseek-v4-flash', 10, 1, '{"temperature":0.7,"maxTokens":2048}', NOW(), NOW()),
('阿里云百炼', 'dashscope', 'DASHSCOPE', 'https://dashscope.aliyuncs.com/compatible-mode/v1', '', 'qwen-max', 5, 0, '{"temperature":0.7,"maxTokens":2048}', NOW(), NOW());

-- 初始化示例工具
INSERT INTO ai_tool (name, code, description, parameters_schema, implementation_bean, enabled, create_time, update_time) VALUES
('查询当前时间', 'current_time', '返回服务器当前时间，支持指定时区。', '{"type":"object","properties":{"timezone":{"type":"string","description":"时区，如 Asia/Shanghai"}},"required":[]}', 'currentTimeTool', 1, NOW(), NOW()),
('简单计算器', 'calculator', '执行基础数学运算。', '{"type":"object","properties":{"expression":{"type":"string","description":"数学表达式，如 1+2*3"}},"required":["expression"]}', 'calculatorTool', 1, NOW(), NOW());
