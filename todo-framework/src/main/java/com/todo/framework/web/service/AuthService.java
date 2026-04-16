package com.todo.framework.web.service;

import com.todo.common.core.domain.R;
import com.todo.framework.provider.JwtTokenProvider;
import com.todo.framework.security.handle.LoginUser;
import com.todo.pojo.dto.AuthDTO;
import com.todo.pojo.vo.AuthVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authDTO.getUsername(), authDTO.getPassword())
            );
        } catch (Exception e) {
            throw new UsernameNotFoundException(e.getMessage());
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 生成token
        String token = jwtTokenProvider.generateAccessToken(loginUser);

        AuthVO authVO = new AuthVO();
        authVO.setId(loginUser.sysUser().getId());
        authVO.setUsername(loginUser.getUsername());
        authVO.setToken(token);

        return R.ok(authVO);
    }
}
