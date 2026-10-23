package com.xy.app.service;

import com.xy.app.pojo.dto.TodoProfileUpdateDTO;

/**
 * 个人资料（t_user 中允许用户自己改的部分：昵称、头像、性别）
 * */
public interface IProfileService {

    /**
     * 修改当前用户的资料，只更新传了值的字段
     * */
    void updateProfile(TodoProfileUpdateDTO dto);
}
