package com.todo.admin.controller;

import com.todo.common.core.domain.R;
import com.todo.common.entity.SysMenu;
import com.todo.common.utils.SecurityUtils;
import com.todo.framework.web.service.AuthService;
import com.todo.common.dto.AuthDTO;
import com.todo.common.entity.SysUser;
import com.todo.common.vo.AuthVO;
import com.todo.system.service.ISysMenuService;
import com.todo.system.service.ISysPermissionService;
import com.todo.system.service.ISysRoleService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 认证控制器
 * */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final ISysPermissionService permissionService;

    private final ISysMenuService menuService;

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
        Set<String> roles = permissionService.getRolePermission(sysUser);
        sysUser.setRoles(roles);

        return R.ok(sysUser);
    }

    /**
     * 获取路由信息
     * */
    @GetMapping("/getRouters")
    public R<?> getRouters() {
        Long userId = SecurityUtils.getUserId();

        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);

        return R.ok(menuService.buildMenus(menus));
    }

    @PostMapping("/test")
    public R<String> test() {
        return R.ok("测试接口");
    }
}