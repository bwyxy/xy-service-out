-- 知识库版本表
CREATE TABLE IF NOT EXISTS ai_knowledge_base_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本 ID',
    kb_id BIGINT NOT NULL COMMENT '所属知识库 ID',
    version VARCHAR(32) NOT NULL COMMENT '版本号，如 v1.0.0',
    description VARCHAR(500) DEFAULT '' COMMENT '版本描述',
    doc_count INT DEFAULT 0 COMMENT '该版本包含的文档数量',
    status TINYINT DEFAULT 0 COMMENT '状态 0-草稿 1-已发布 2-已归档',
    published_time DATETIME COMMENT '发布时间',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    UNIQUE INDEX uk_kb_version (kb_id, version),
    INDEX idx_kb_status (kb_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 知识库版本表';

-- 知识库权限表
CREATE TABLE IF NOT EXISTS ai_knowledge_base_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限 ID',
    kb_id BIGINT NOT NULL COMMENT '所属知识库 ID',
    target_type VARCHAR(32) NOT NULL COMMENT '授权对象类型：ROLE-角色 / USER-用户',
    target_id BIGINT NOT NULL COMMENT '角色 ID 或用户 ID',
    permission VARCHAR(32) NOT NULL COMMENT '权限类型：READ-只读 / MANAGE-管理',
    create_time DATETIME COMMENT '创建时间',
    UNIQUE INDEX uk_kb_target (kb_id, target_type, target_id),
    INDEX idx_target (target_type, target_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 知识库权限表';
