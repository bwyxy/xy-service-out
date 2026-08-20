package com.chat.ai.enums;

import lombok.Getter;

/**
 * 会话状态
 */
@Getter
public enum ConversationStatus {

    ACTIVE(0, "正常"),
    ARCHIVED(1, "归档"),
    DELETED(2, "删除");

    private final int code;
    private final String desc;

    ConversationStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
