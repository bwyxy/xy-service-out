package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.mapper.UserMapper;
import com.chat.fg.service.IUserService;
import com.xy.common.entity.User;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public User getByPhoneOrEmail(String phone, String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone)
                .or()
                .eq(User::getEmail, email);
        return getOne(wrapper);
    }

    @Override
    public User getUserInfo(Long uid) {
        return getById(uid);
    }
}