package com.xy.common.enums;

import lombok.Getter;

/**
 * 公共状态类
 * */
@Getter
public enum BusinessStatus {

    normal("0", "正常"),

    stop("1", "停止");

    private final String code;

    private final String desc;
    BusinessStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
