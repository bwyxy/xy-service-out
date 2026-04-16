package com.todo.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.todo.pojo.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    boolean checkPassword(String rawPassword, String encodedPassword);

    String encodePassword(String rawPassword);
}
