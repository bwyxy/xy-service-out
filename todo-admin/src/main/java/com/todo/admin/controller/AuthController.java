package com.todo.admin.controller;

import com.todo.common.core.domain.R;
import com.todo.framework.web.service.AuthService;
import com.todo.pojo.dto.AuthDTO;
import com.todo.pojo.vo.AuthVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * */
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

    @PostMapping("/test")
    public R<String> test() {
        return R.ok("测试接口");
    }
}