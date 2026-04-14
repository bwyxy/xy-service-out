package com.todo.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.todo.pojo.entity.SysUser;

/**
 * 用户表 逻辑层
 * */
public interface SysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    boolean checkPassword(String rawPassword, String encodedPassword);

    String encodePassword(String rawPassword);
}
