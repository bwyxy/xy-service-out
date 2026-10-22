package com.xy.app.controller;

import com.xy.app.pojo.vo.WeChatLoginVo;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.IWeChatLoginService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.enums.ResultCode;
import com.xy.common.entity.User;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.common.web.service.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof TodoUserDetails todoUserDetails)) {
            return R.error(ResultCode.UNAUTHORIZED);
        }

        User user = todoUserDetails.getUser();
        user.setPassword(null);

        return R.ok(user);
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
