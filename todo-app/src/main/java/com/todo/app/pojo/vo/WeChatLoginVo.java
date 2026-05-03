package com.todo.app.pojo.vo;

import lombok.Data;

/**
 * 微信小程序登录 response
 * */
@Data
public class WeChatLoginVo {

    private Long userId;

    private String token;

    private String openid;
}
