package com.xy.app.service;

import com.xy.app.pojo.vo.WeChatLoginVo;

/**
 * 微信小程序登录
 * */
public interface IWeChatLoginService {

    public WeChatLoginVo login(String code);
}
