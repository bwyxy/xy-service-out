package com.todo.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.todo.common.entity.SysUser;

public interface ISysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    boolean checkPassword(String rawPassword, String encodedPassword);

    String encodePassword(String rawPassword);

    /**
     * 根据 openid 查询用户信息（返回完整用户对象）
     * @param openId 微信小程序用户唯一标识
     * @return 用户对象，不存在则返回 null
     * */
    SysUser getUserByOpenId(String openId);

    /**
     * 添加用户
     * */
    Long addUser(SysUser sysUser);

}
