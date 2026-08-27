package com.xy.common.enums.forum;

import lombok.Getter;

/**
 * 举报类型
 * */
@Getter
public enum ReportType {

    POST(1, "帖子"),

    COMMENT(2, "评论"),

    USER(3, "用户");

    private final  Integer code;
    private final String desc;
    ReportType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
