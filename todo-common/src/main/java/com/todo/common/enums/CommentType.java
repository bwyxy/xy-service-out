package com.todo.common.enums;

import lombok.Getter;

/**
 * 评论类型枚举
 * */
@Getter
public enum CommentType {

    ARTICLE("article", "文章评论"),

    MESSAGE("message", "留言板");

    private final String code;

    private final String desc;
    CommentType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }

    public static CommentType fromCode(String code) {
        for (CommentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知评论类型: " + code);
    }
}
