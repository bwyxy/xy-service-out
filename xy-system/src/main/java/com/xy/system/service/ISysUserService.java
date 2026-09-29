package com.xy.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.entity.SysUser;

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

    /**
     * 新增管理用户
     * @param sysUser 用户信息
     * @return 用户ID
     * */
    Long insertUser(SysUser sysUser);

    /**
     * 修改管理用户
     * @param sysUser 用户信息
     * @return 是否成功
     * */
    boolean updateUser(SysUser sysUser);

    /**
     * 删除管理用户
     * @param userId 用户ID
     * @return 是否成功
     * */
    boolean deleteUser(Long userId);

    /**
     * 根据用户ID查询用户信息
     * @param userId 用户ID
     * @return 用户信息
     * */
    SysUser selectUserById(Long userId);

    /**
     * 校验用户名是否唯一
     * @param sysUser 用户信息
     * @return true 唯一 / false 不唯一
     * */
    boolean checkUsernameUnique(SysUser sysUser);

    /**
     * 分页查询管理用户列表
     * @param user 查询条件
     * @return 分页结果
     * */
    com.xy.common.core.domain.PageResult<SysUser> selectUserList(SysUser user);

    /**
     * 校验用户是否拥有超级管理员角色
     * @param userId 用户ID
     * @return true 是超级管理员
     * */
    boolean isSuperAdmin(Long userId);

    /**
     * 修改个人资料（仅允许修改昵称、性别、邮箱、手机号、头像、简介）
     * @param sysUser 用户信息（id 必填）
     * @return 是否成功
     * */
    boolean updateUserProfile(SysUser sysUser);

    /**
     * 修改用户密码（校验旧密码）
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     * */
    boolean updateUserPwd(Long userId, String oldPassword, String newPassword);

}
