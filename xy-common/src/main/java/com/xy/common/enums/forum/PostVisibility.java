package com.xy.common.enums.forum;

import lombok.Getter;

/**
 * 帖子可见
 * */
@Getter
public enum PostVisibility {

    overt(0, "公开"),

    visibleToFriends(1, "好友可见"),

    invisible(2, "私密");

    private Integer code;
    private String desc;
    PostVisibility(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
