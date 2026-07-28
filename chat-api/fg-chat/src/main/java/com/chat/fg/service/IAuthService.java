package com.chat.fg.service;

import com.chat.fg.pojo.dto.RegisterDTO;

/**
 * 认证
 * */
public interface IAuthService {

    /**
     * 注册用户
     * */
    public Long registerUser(RegisterDTO registerDTO);
}
