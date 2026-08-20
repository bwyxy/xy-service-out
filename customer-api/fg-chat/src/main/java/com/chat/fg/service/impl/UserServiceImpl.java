package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.pojo.dto.EditPasswordDTO;
import com.chat.fg.pojo.vo.UserInfoVO;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.EncryptionUtils;
import com.xy.common.utils.StringUtils;
import com.xy.framework.web.service.VerifyService;
import com.xy.system.mapper.UserMapper;
import com.chat.fg.service.IUserService;
import com.xy.common.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private VerifyService verifyService;

    @Autowired
    private EncryptionUtils encryptionUtils;

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

    /**
     * 更新用户头像
     * */
    @Override
    public void updateUserAvatar(String avatar) {
        Long uId = SecurityUtils.getUId();

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now();

        updateWrapper.eq(User::getUId, uId);
        updateWrapper.set(User::getAvatar, avatar);
        updateWrapper.set(User::getUpdateTime, now);

        update(updateWrapper);
    }

    /**
     * 更新用户信息
     * */
    @Override
    public void updateUserInfo(User user) {
        Long uId = SecurityUtils.getUId();

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now();

        updateWrapper.eq(User::getUId, uId);
        if (user.getNickname() == null || StringUtils.isEmpty(user.getNickname())) {
            user.setNickname("用户" + uId);
        }

        if (user.getSex() != null && !StringUtils.isEmpty((user.getSex()))) {
            updateWrapper.set(User::getSex, user.getSex());
        }

        updateWrapper.set(User::getNickname, user.getNickname())
                    .set(User::getUpdateTime, now);

        update(updateWrapper);
    }

    /**
     * 修改密码
     * */
    @Override
    public void editPassword(EditPasswordDTO editPasswordDTO) {

        if (!editPasswordDTO.isNewAndConfirmNewPassword()) {
            throw new BusinessException("两次密码不一致, 请确认您输入的密码");
        };

        String email = editPasswordDTO.getEmail();
        String emailCode = editPasswordDTO.getEmailCode();

        if (!verifyService.verifyCode(email, emailCode)) {
            throw new BusinessException("邮箱验证码不正确，请确认您的验证码");
        }

        Long uId = SecurityUtils.getUId();

        String pwd = getPassword(uId);

        if (!encryptionUtils.matches(editPasswordDTO.getOldPassword(), pwd)) {
            throw new BusinessException("旧密码不一致");
        }

        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUId, uId);

        String password = encryptionUtils.encode(editPasswordDTO.getNewPassword());

        updateWrapper.set(User::getPassword, password)
                .set(User::getUpdateTime, now);

        update(updateWrapper);
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

    private String getPassword(Long uid) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(User::getUId, uid);

        User user = getOne(wrapper);

        return user.getPassword();
    }
}