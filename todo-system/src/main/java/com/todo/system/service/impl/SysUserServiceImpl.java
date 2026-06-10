package com.todo.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.todo.common.entity.SysUser;
import com.todo.common.utils.StringUtils;
import com.todo.system.mapper.SysUserMapper;
import com.todo.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 根据 openid 查询用户信息（返回完整用户对象）
     * @param openId 微信小程序用户唯一标识
     * @return 用户对象，不存在则返回 null
     * */
    @Override
    public SysUser getUserByOpenId(String openId) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysUser::getWxOpenId, openId);

        return this.getOne(queryWrapper);
    }

    /**
     * 添加用户
     * */
    @Override
    public Long addUser(SysUser sysUser) {

        boolean success = save(sysUser);

        if (success) {
            Long userId = sysUser.getId();

            if (StringUtils.isEmpty(sysUser.getUsername())) {
                String username = String.valueOf(userId);
                sysUser.setUsername(username);
                this.lambdaUpdate()
                        .eq(SysUser::getId, userId)
                        .set(SysUser::getUsername, username)
                        .update();
            }

            return sysUser.getId();
        }

        throw new RuntimeException("添加用户失败");
    }
}
