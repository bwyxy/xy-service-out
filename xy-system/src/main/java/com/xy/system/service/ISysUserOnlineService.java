package com.xy.system.service;

import com.xy.common.core.domain.model.LoginUser;
import com.xy.common.entity.SysUserOnline;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 在线用户 服务层
 * */
public interface ISysUserOnlineService {

    /**
     * 保存在线用户信息
     *
     * @param user 登录用户信息
     * @param token 登录令牌
     * @param request 请求对象
     * */
    void saveOnlineUser(LoginUser user, String token, HttpServletRequest request);

    /**
     * 查询在线用户列表
     *
     * @param ipaddr IP地址
     * @param username 用户名
     * @return 在线用户列表
     * */
    List<SysUserOnline> listOnlineUsers(String ipaddr, String username);

    /**
     * 管理员强制退出用户
     *
     * @param tokenId token 标识
     * */
    void forceLogout(String tokenId);

    /**
     * 移除在线用户（用户退出登录）
     *
     * @param tokenId token 标识
     * */
    void removeOnlineUser(String tokenId);
}
