package com.todo.framework.web.service;

import com.todo.common.core.domain.R;
import com.todo.framework.provider.JwtTokenProvider;
import com.todo.common.core.domain.model.LoginUser;
import com.todo.common.dto.AuthDTO;
import com.todo.common.entity.SysUser;
import com.todo.common.vo.AuthVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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


    /**
     * 用户登录
     * */
    public R<AuthVO> login(AuthDTO authDTO) {

        // 用户验证
        Authentication authentication = null;
        log.warn("登录DTO: {}", authDTO);
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authDTO.getUsername(), authDTO.getPassword())
            );
        } catch (Exception e) {
            log.warn("登录认证失败: {}", e.getMessage());
            throw new UsernameNotFoundException(e.getMessage());
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 生成token
        String token = jwtTokenProvider.generateAccessToken(loginUser);

        AuthVO authVO = new AuthVO();
        authVO.setId(loginUser.getUserId());
        authVO.setUsername(loginUser.getUsername());
        authVO.setToken(token);

        return R.ok(authVO);
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
