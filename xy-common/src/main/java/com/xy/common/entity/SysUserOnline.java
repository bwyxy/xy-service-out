package com.xy.common.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 在线用户信息（Redis/VO 使用）
 * */
@Data
public class SysUserOnline implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户会话 token 标识
     * */
    private String tokenId;

    /**
     * 用户名
     * */
    private String username;

    /**
     * 部门名称
     * */
    private String deptName;

    /**
     * 登录IP地址
     * */
    private String ipaddr;

    /**
     * 登录地点
     * */
    private String loginLocation;

    /**
     * 浏览器类型
     * */
    private String browser;

    /**
     * 操作系统
     * */
    private String os;

    /**
     * 登录时间
     * */
    private Long loginTime;

    /**
     * 角色名称
     * */
    private String roleName;
}
