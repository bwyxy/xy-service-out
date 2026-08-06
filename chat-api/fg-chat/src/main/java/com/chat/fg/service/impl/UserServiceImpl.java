package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.pojo.vo.UserInfoVO;
import com.xy.system.mapper.UserMapper;
import com.chat.fg.service.IUserService;
import com.xy.common.entity.User;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 根据 uId查询用户信息
     * */
    @Override
    public UserInfoVO getUserInfoByUId(Long userId) {

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUId, userId);

        User user = getOne(queryWrapper);

        UserInfoVO userInfoVO = new UserInfoVO();

        userInfoVO.setUId(user.getUId());
        userInfoVO.setNickname(user.getNickname());
        userInfoVO.setAvatar(user.getAvatar());
        userInfoVO.setSex(user.getSex());

        return userInfoVO;
    }

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