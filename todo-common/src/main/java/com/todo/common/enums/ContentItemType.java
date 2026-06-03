package com.todo.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 通用内容项类型
 * */
@Getter
public enum ContentItemType {
    link("link", "友链"),

    work("work", "工作经验"),

    project("project", "项目经验"),

    notice("notice", "公告"),

    meProject("meProject", "我的项目"),

    portfolio("portfolio", "作品集");

    private final String code;

    private final String desc;
    ContentItemType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getCode() { return code; }

    @JsonCreator
    public static ContentItemType fromCode(String code) {
        for (ContentItemType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知通用项内容类型: " + code);
    }
}
