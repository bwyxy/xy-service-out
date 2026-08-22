package com.xy.common.enums.ai;

import lombok.Getter;

/**
 * 知识库状态
 */
@Getter
public enum KnowledgeBaseStatus {

    ENABLED(0, "启用"),
    DISABLED(1, "禁用");

    private final int code;
    private final String desc;

    KnowledgeBaseStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
