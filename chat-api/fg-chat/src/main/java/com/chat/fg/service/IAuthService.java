package com.chat.fg.service;

import com.chat.fg.pojo.dto.LoginDTO;
import com.chat.fg.pojo.dto.RegisterDTO;

import java.util.Map;

/**
 * 认证
 * */
public interface IAuthService {

    /**
     * 注册用户
     * */
    Long registerUser(RegisterDTO registerDTO);

    /**
     * 登录
     * */
    Map<String, Object> login(LoginDTO loginDTO);
}