package com.chat.ai.enums;

import lombok.Getter;

/**
 * AI 消息角色
 */
@Getter
public enum MessageRole {

    SYSTEM("system", "系统"),
    USER("user", "用户"),
    ASSISTANT("assistant", "助手"),
    TOOL("tool", "工具");

    private final String code;
    private final String desc;

    MessageRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MessageRole of(String code) {
        for (MessageRole value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return USER;
    }
}
