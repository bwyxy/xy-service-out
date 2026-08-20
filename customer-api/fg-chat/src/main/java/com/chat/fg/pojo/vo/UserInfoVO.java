package com.chat.fg.pojo.vo;

import lombok.Data;

@Data
public class UserInfoVO {

    /**
     * 用户 uId
     * */
    private Long uId;

    /**
     * 用户 昵称
     * */
    private String nickname;

    /**
     * 用户头像
     * */
    private String avatar;

    /**
     * 性别
     * */
    private String sex;
}
