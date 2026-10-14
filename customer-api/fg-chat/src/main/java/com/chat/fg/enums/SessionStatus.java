package com.chat.fg.enums;

import lombok.Getter;

/**
 * 登录会话状态
 */
@Getter
public enum SessionStatus {

    /** 有效活跃会话 */
    ACTIVE("ACTIVE", "在线"),

    /** 被同类型设备登录强制下线 */
    KICKED("KICKED", "被顶下线"),

    /** 用户主动登出 */
    LOGOUT("LOGOUT", "已登出");

    private final String code;
    private final String label;

    SessionStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}