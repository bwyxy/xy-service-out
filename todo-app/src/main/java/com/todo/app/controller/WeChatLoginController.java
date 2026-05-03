package com.todo.app.controller;

import com.todo.app.pojo.vo.WeChatLoginVo;
import com.todo.app.service.IWeChatLoginService;
import com.todo.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat")
public class WeChatLoginController {

    @Autowired
    private IWeChatLoginService weChatLoginService;

    /**
     * 微信小程序登录
     * */
    @PostMapping("/login")
    public R<?> weChatLogin(@RequestParam String code) {
        WeChatLoginVo weChatLoginVo = weChatLoginService.login(code);

        return R.ok(weChatLoginVo);
    }
}
