package com.xy.common.enums;

import lombok.Getter;

/**
 * 用于是否同意某件事
 * */
@Getter
public enum YesNoEnum {

    NO(0, "否"),

    YES(1, "是");

    private final Integer code;
    private final String desc;

    YesNoEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
