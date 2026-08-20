package com.chat.fg.controller;

import com.chat.fg.pojo.vo.UserInfoVO;
import com.chat.fg.service.IUserService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /**
     * 根据用户 Id 查询用户信息
     * */
    @GetMapping("/{uId}")
    public R<UserInfoVO> getUserInfoByUId(@PathVariable Long uId) {
        UserInfoVO userInfoVO = userService.getUserInfoByUId(uId);

        return R.ok(userInfoVO);
    }
}