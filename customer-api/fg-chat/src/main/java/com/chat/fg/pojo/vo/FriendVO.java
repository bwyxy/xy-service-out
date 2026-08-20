package com.chat.fg.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友 VO
 * */
@Data
public class FriendVO {

    /**
     * 好友关系ID
     * */
    private Long id;

    /**
     * 好友用户ID
     * */
    private Long friendId;

    /**
     * 好友昵称
     * */
    private String nickname;

    /**
     * 好友头像
     * */
    private String avatar;

    /**
     * 状态 0-正常 1-拉黑
     * */
    private String status;

    /**
     * 添加时间
     * */
    private LocalDateTime createTime;
}
