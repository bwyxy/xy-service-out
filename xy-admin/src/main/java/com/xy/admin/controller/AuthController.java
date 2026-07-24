package com.xy.admin.controller;

import com.xy.common.core.domain.R;
import com.xy.common.entity.SysMenu;
import com.xy.common.utils.SecurityUtils;
import com.xy.framework.web.service.AuthService;
import com.xy.common.dto.AuthDTO;
import com.xy.common.entity.SysUser;
import com.xy.common.vo.AuthVO;
import com.xy.system.service.ISysMenuService;
import com.xy.system.service.ISysPermissionService;
import com.xy.system.service.ISysRoleService;
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