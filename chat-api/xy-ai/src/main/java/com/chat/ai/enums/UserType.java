package com.chat.ai.enums;

import lombok.Getter;

/**
 * AI 模块用户类型
 */
@Getter
public enum UserType {

    CLIENT("CLIENT", "客户端用户"),
    ADMIN("ADMIN", "管理员");

    private final String code;
    private final String desc;

    UserType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserType of(String code) {
        for (UserType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return CLIENT;
    }
}
