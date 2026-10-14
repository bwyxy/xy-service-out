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
     *
     * @param loginDTO  登录参数
     * @param ip        登录 IP
     * @param userAgent 客户端 User-Agent（用于缺省推断设备类型）
     * */
    Map<String, Object> login(LoginDTO loginDTO, String ip, String userAgent);

    /**
     * 登出（仅可结束自身 token 对应的会话）
     *
     * @param token 当前请求携带的会话 token
     * */
    void logout(String token);
}