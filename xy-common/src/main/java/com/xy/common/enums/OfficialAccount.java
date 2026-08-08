package com.xy.common.enums;

import lombok.Getter;

/**
 * 系统通知 官方账号 枚举
 * */
@Getter
public enum OfficialAccount {

    systemAdmin("system_admin", "系统官方账号"),

    fgAI("fg_ai", "飞鸽AI助手"),

    systemNotice("system_notice", "系统通知账号");

    private final String code;
    private final String desc;
    OfficialAccount(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
