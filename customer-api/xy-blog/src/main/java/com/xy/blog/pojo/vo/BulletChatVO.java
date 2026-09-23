package com.xy.blog.pojo.vo;

import lombok.Data;

@Data
public class BulletChatVO {

    private Long id;

    private String avatarEmoji;

    private String nickname;

    private String ipAddress;

    private String content;
}
