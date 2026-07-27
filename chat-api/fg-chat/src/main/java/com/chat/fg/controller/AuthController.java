package com.chat.fg.controller;

import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.framework.web.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证
 * */
@RestController
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private VerifyService verifyService;

    /**
     * 发送验证码
     * */
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
