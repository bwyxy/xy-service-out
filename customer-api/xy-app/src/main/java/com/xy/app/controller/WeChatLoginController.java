package com.xy.app.controller;

import com.xy.app.pojo.vo.WeChatLoginVo;
import com.xy.app.service.IWeChatLoginService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysUser;
import com.xy.framework.provider.JwtTokenProvider;
import com.xy.framework.web.service.AuthService;
import com.xy.framework.web.service.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wechat")
public class WeChatLoginController {

    @Autowired
    private IWeChatLoginService weChatLoginService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private AuthService authService;

    /**
     * 微信小程序登录
     * */
    @Anonymous
    @PostMapping("/login")
    public R<?> weChatLogin(@RequestParam String code) {
        WeChatLoginVo weChatLoginVo = weChatLoginService.login(code);

        return R.ok(weChatLoginVo);
    }

    /**
     * 获取登录用户信息
     * */
    @GetMapping("/getInfo")
    public R<?> getInfo() {
        SysUser sysUser = authService.getUserInfo();
        sysUser.setPassword(null);

        return R.ok(sysUser);
    }

    /**
     * 退出登录
     * */
    @PostMapping("/logout")
    public R<?> weChatLogout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring("Bearer ".length());

        long remainingExpiration = jwtTokenProvider.getRemainingExpiration(token);

        tokenBlacklistService.blacklist(token, remainingExpiration);

        return R.ok("退出成功");
    }
}
