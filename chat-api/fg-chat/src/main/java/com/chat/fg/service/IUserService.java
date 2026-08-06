package com.chat.fg.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.fg.pojo.vo.UserInfoVO;
import com.xy.common.entity.User;

/**
 * 用户 逻辑层
 * */
public interface IUserService extends IService<User> {

    /**
     * 根据 uId查询用户信息
     * */
    UserInfoVO getUserInfoByUId(Long userId);

    /**
     * 根据手机号或邮箱查询用户
     */
    User getByPhoneOrEmail(String phone, String email);

    /**
     * 根据UID获取用户信息
     */
    User getUserInfo(Long uid);
}