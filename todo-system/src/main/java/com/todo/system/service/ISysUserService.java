package com.todo.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.todo.common.entity.SysUser;

public interface ISysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    boolean checkPassword(String rawPassword, String encodedPassword);

    String encodePassword(String rawPassword);
}
