package com.chat.fg.controller;

import com.chat.fg.pojo.dto.LoginDTO;
import com.chat.fg.pojo.dto.RegisterDTO;
import com.chat.fg.security.ChatUserDetails;
import com.chat.fg.service.IAuthService;
import com.chat.fg.service.IUserService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.entity.User;
import com.xy.framework.web.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private VerifyService verifyService;

    @Autowired
    private IAuthService authService;

    @Autowired
    private IUserService userService;

    /**
     * 注册
     */
    @Anonymous
    @PostMapping("/auth/register")
    public R<?> register(RegisterDTO registerDTO) {
        Long uId = authService.registerUser(registerDTO);
        return R.ok(uId);
    }

    /**
     * 登录
     */
    @Anonymous
    @PostMapping("/chat/login")
    public R<?> login(LoginDTO loginDTO) {
        Map<String, Object> result = authService.login(loginDTO);
        return R.ok(result);
    }

    /**
     * 获取当前登录用户信息
     */
    @PostMapping("/auth/userinfo")
    public R<?> getUserInfo() {
        // 聊天用户使用 ChatUserDetails，直接取 principal
        ChatUserDetails principal = (ChatUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        User user = userService.getUserInfo(principal.getUId());

        Map<String, Object> result = new HashMap<>();
        result.put("uid", String.valueOf(user.getUId()));
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("phone", user.getPhone());
        result.put("email", user.getEmail());
        result.put("sex", user.getSex());
        return R.ok(result);
    }

    /**
     * 发送验证码
     */
    @Anonymous
    @PostMapping("/send/code")
    public R<?> sendCode(@RequestParam String email) {
        try {
            verifyService.sendVerifyCode(email);
        } catch (IllegalArgumentException e) {
            return R.error(404, e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return R.ok("发送成功");
    }
}