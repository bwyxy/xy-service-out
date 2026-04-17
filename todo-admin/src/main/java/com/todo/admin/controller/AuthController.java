package com.todo.admin.controller;

import com.todo.common.core.domain.R;
import com.todo.framework.web.service.AuthService;
import com.todo.common.dto.AuthDTO;
import com.todo.common.entity.SysUser;
import com.todo.common.vo.AuthVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     * */
    @PostMapping("/login")
    public R<AuthVO> login(@Valid @RequestBody AuthDTO authDTO) {
        return authService.login(authDTO);
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

    @PostMapping("/test")
    public R<String> test() {
        return R.ok("测试接口");
    }
}