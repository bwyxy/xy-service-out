package com.chat.fg.pojo.enums;

import lombok.Getter;

/**
 * 群成员身份
 * */
@Getter
public enum GroupMemberRole {

    regular(0, "普通成员"),

    admin(1, "管理员"),

    owner(2, "群主");

    private final Integer code;
    private final String desc;
    GroupMemberRole(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
