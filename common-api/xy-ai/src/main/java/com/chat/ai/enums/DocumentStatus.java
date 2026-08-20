package com.chat.ai.enums;

import lombok.Getter;

/**
 * 知识库文档处理状态
 */
@Getter
public enum DocumentStatus {

    PROCESSING(0, "处理中"),
    COMPLETED(1, "已完成"),
    FAILED(2, "失败");

    private final int code;
    private final String desc;

    DocumentStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
