package com.xy.admin.controller;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysLogininfor;
import com.xy.common.entity.SysMenu;
import com.xy.common.ip.AddressUtils;
import com.xy.common.ip.IpUtils;
import com.xy.common.utils.SecurityUtils;
import com.xy.common.utils.StringUtils;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.framework.web.service.AuthService;
import com.xy.common.dto.AuthDTO;
import com.xy.common.entity.SysUser;
import com.xy.common.vo.AuthVO;
import com.xy.common.web.service.TokenBlacklistService;
import com.xy.system.service.ISysLogininforService;
import com.xy.system.service.ISysMenuService;
import com.xy.system.service.ISysPermissionService;
import com.xy.system.service.ISysRoleService;
import com.xy.system.service.ISysUserOnlineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    private final TokenBlacklistService tokenBlacklistService;

    private final ISysUserOnlineService userOnlineService;

    private final ISysLogininforService logininforService;

    private final JwtTokenProvider jwtTokenProvider;

    private static final String ONLINE_TOKEN_KEY = "online:token:";

    /**
     * 用户登录
     * */
    @PostMapping("/login")
    public R<AuthVO> login(@Valid @RequestBody AuthDTO authDTO, HttpServletRequest request) {
        return authService.login(authDTO, request);
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

    /**
     * 安全退出
     * */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            long remainingTime = jwtTokenProvider.getRemainingExpiration(token);
            tokenBlacklistService.blacklist(token, remainingTime);
            userOnlineService.removeOnlineUser(token);
        }

        String userName = "";
        try {
            userName = SecurityUtils.getLoginUser().getUsername();
        } catch (Exception e) {
            log.warn("获取当前登录用户失败: {}", e.getMessage());
        }

        recordLogoutLog(userName, request);

        return R.ok();
    }

    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.isNotEmpty(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }

    private void recordLogoutLog(String userName, HttpServletRequest request) {
        String ipaddr = IpUtils.getIpAddr(request);
        UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));

        SysLogininfor logininfor = new SysLogininfor();
        logininfor.setUserName(userName);
        logininfor.setIpaddr(ipaddr);
        logininfor.setLoginLocation(AddressUtils.getRealAddressByIP(ipaddr));
        logininfor.setBrowser(userAgent.getBrowser().getName());
        logininfor.setOs(userAgent.getOs().getName());
        logininfor.setStatus("0");
        logininfor.setMsg("退出成功");
        logininfor.setLoginTime(LocalDateTime.now());

        logininforService.save(logininfor);
    }
}