package com.chat.fg.service.impl;

import com.chat.fg.pojo.dto.LoginDTO;
import com.chat.fg.pojo.dto.RegisterDTO;
import com.chat.fg.security.ChatUserDetails;
import com.chat.fg.service.IAuthService;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.WKUserTokenRequest;
import com.xy.common.client.dto.response.ApiResult;
import com.xy.common.client.dto.response.IMAddrResponse;
import com.xy.common.entity.User;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.EncryptionUtils;
import com.xy.common.utils.StringUtils;
import com.xy.framework.provider.JwtTokenProvider;
import com.xy.framework.web.service.VerifyService;
import com.xy.system.service.wk.WKClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private VerifyService verifyService;

    @Autowired
    private EncryptionUtils encryptionUtils;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private WKClientService wkClientService;

    @Autowired
    private WuKongClient wuKongClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 注册用户
     */
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

        try {
            userServiceImpl.save(user);

            Long uId = user.getUId();

            String content = "欢迎您使用飞鸽即时通讯。如果您在使用过程中有任何问题或建议，记得给我发消息反馈哦!";
            wkClientService.sendSystemMessage(uId, content);
            return uId;
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("uk_phone")) {
                throw new BusinessException("手机号已被注册");
            } else if (e.getMessage().contains("uk_email")) {
                throw new BusinessException("邮箱已被注册");
            }
            throw e;
        }
    }

    /**
     * 登录
     */
    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        String account = loginDTO.getAccount();

        // 1. 根据手机号或邮箱查询聊天用户（t_user 表）
        User user = userServiceImpl.getByPhoneOrEmail(account, account);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }

        // 2. 校验密码
        if (!encryptionUtils.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        Long uId = user.getUId();

        // 3. 生成 JWT
        ChatUserDetails chatUserDetails = new ChatUserDetails(user);
        String jwtToken = jwtTokenProvider.generateAccessToken(chatUserDetails);

        // 4. 获取/更新悟空IM Token
        String uid = String.valueOf(uId);
        WKUserTokenRequest tokenRequest = new WKUserTokenRequest();
        tokenRequest.setUid(uid);
        tokenRequest.setToken(jwtToken);
        tokenRequest.setDeviceFlag(1);
        tokenRequest.setDeviceLevel(0);

        ApiResult tokenResult = wuKongClient.resetUserToken(tokenRequest);
        if (tokenResult.getStatus() != 200) {
            throw new BusinessException("获取IM连接失败: " + tokenResult.getMsg());
        }

        // 5. 获取用户IM 地址
        IMAddrResponse imAddrResponse = wuKongClient.getRoute(uId);

        // 6. 组装返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("token", jwtToken);
        result.put("nickname", user.getNickname());
        result.put("imAddr", imAddrResponse);
        return result;
    }
}