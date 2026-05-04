package com.todo.app.service;

import com.todo.app.pojo.vo.WeChatLoginVo;

/**
 * 微信小程序登录
 * */
public interface IWeChatLoginService {

    public WeChatLoginVo login(String code);
}
