package com.xy.framework.web.service;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysLogininfor;
import com.xy.common.ip.AddressUtils;
import com.xy.common.ip.IpUtils;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.common.core.domain.model.LoginUser;
import com.xy.common.dto.AuthDTO;
import com.xy.common.entity.SysUser;
import com.xy.common.vo.AuthVO;
import com.xy.system.service.ISysLogininforService;
import com.xy.system.service.ISysUserOnlineService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final JwtTokenProvider jwtTokenProvider;

    private final ISysUserOnlineService userOnlineService;

    private final ISysLogininforService logininforService;

    /**
     * 用户登录
     * */
    public R<AuthVO> login(AuthDTO authDTO, HttpServletRequest request) {

        // 用户验证
        Authentication authentication = null;
        log.warn("登录DTO: {}", authDTO);
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authDTO.getUsername(), authDTO.getPassword())
            );
        } catch (Exception e) {
            log.warn("登录认证失败: {}", e.getMessage());
            recordLogininfor(authDTO.getUsername(), "1", "登录失败，" + e.getMessage(), request);
            throw new UsernameNotFoundException(e.getMessage());
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 生成token
        String token = jwtTokenProvider.generateAccessToken(loginUser);

        userOnlineService.saveOnlineUser(loginUser, token, request);

        recordLogininfor(loginUser.getUsername(), "0", "登录成功", request);

        AuthVO authVO = new AuthVO();
        authVO.setId(loginUser.getUserId());
        authVO.setUsername(loginUser.getUsername());
        authVO.setToken(token);

        return R.ok(authVO);
    }

    /**
     * 记录登录日志
     * */
    private void recordLogininfor(String userName, String status, String msg, HttpServletRequest request) {
        String ipaddr = IpUtils.getIpAddr(request);
        UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));

        SysLogininfor logininfor = new SysLogininfor();
        logininfor.setUserName(userName);
        logininfor.setIpaddr(ipaddr);
        logininfor.setLoginLocation(AddressUtils.getRealAddressByIP(ipaddr));
        logininfor.setBrowser(userAgent.getBrowser().getName());
        logininfor.setOs(userAgent.getOs().getName());
        logininfor.setStatus(status);
        logininfor.setMsg(msg);
        logininfor.setLoginTime(LocalDateTime.now());

        logininforService.save(logininfor);
    }

    /**
     * 用户信息
     * */
    public SysUser getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        return loginUser.getSysUser();
    }
}
