package com.xy.common.enums;

import lombok.Getter;

/**
 * 好友请求状态枚举
 * */
@Getter
public enum FriendRequestStatus {

    PENDING(0, "待处理"),
    ACCEPTED(1, "已同意"),
    REJECTED(2, "已拒绝"),
    CANCELED(3, "已取消"),
    EXPIRED(4, "已过期");

    private final Integer code;
    private final String desc;

    FriendRequestStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FriendRequestStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FriendRequestStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
