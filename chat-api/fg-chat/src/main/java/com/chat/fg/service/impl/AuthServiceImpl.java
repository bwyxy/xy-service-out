package com.chat.fg.service.impl;

import com.chat.fg.pojo.dto.RegisterDTO;
import com.chat.fg.service.IAuthService;
import com.xy.common.entity.User;
import com.xy.common.utils.EncryptionUtils;
import com.xy.common.utils.StringUtils;
import com.xy.framework.web.service.VerifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private VerifyService verifyService;

    @Autowired
    private EncryptionUtils encryptionUtils;

    @Autowired
    private UserServiceImpl UserServiceImpl;
    /**
     * 注册用户
     * */
    @Override
    public Long registerUser(RegisterDTO registerDTO) {

        if (!registerDTO.isPasswordConfirmPassword()) {
            throw new SecurityException("两次密码不一致, 请确认您输入的密码");
        }

        String email = registerDTO.getEmail();
        String emailCode = registerDTO.getEmailCode();
        if (!verifyService.verifyCode(email, emailCode)) {
            throw new SecurityException("邮箱验证码不正确，请确认您的验证码");
        }

        String password = encryptionUtils.encode(registerDTO.getPassword());
        LocalDateTime now = LocalDateTime.now();
        User user = new User();

        if (registerDTO.getNickname() == null || StringUtils.isEmpty(registerDTO.getNickname())) {
            user.setNickname("飞鸽用户");
        } else {
            user.setNickname(registerDTO.getNickname());
        }
        user.setPassword(password);
        user.setPhone(registerDTO.getPhone());
        user.setEmail(email);
        user.setSex(registerDTO.getSex());
        user.setCreateTime(now);
        user.setUpdateTime(now);

        UserServiceImpl.save(user);

        return user.getUId();
    }
}
