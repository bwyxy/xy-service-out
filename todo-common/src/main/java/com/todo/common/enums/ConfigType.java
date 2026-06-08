package com.todo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 内容配置枚举
 * */
@Getter
public enum ConfigType {

    userMenu("userMenu", "用户菜单"),

    utils("utils", "工具配置"),

    tabbar("tabbar", "底部栏"),

    swiper("swiper", "轮播图");

    private final String code;

    private final String desc;

    ConfigType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getCode() { return code; }

    @JsonCreator
    public static ConfigType fromCode(String code) {
        for (ConfigType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知配置类型: " + code);
    }
}
