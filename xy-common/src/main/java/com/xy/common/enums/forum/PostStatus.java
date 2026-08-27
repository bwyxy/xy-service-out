package com.xy.common.enums.forum;

import lombok.Getter;

/**
 * 帖子状态
 * */
@Getter
public enum PostStatus {

    draft(0, "草稿"),

    underReview(1, "审核中"),

    published(2, "已发布"),

    refuse(3, "拒绝"),

    remove(4, "下架");

    private final int code;

    private final String desc;
    PostStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
