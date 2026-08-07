package com.xy.common.enums;

import lombok.Getter;

/**
 * 通知类型枚举
 * */
@Getter
public enum NotificationType {

    FRIEND_REQUEST(1, "好友请求"),
    FRIEND_ACCEPTED(2, "好友同意"),
    SYSTEM(3, "系统通知");

    private final Integer code;
    private final String desc;

    NotificationType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotificationType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (NotificationType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
