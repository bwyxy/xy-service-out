package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xy.common.core.domain.BaseEntity;
import com.xy.common.utils.SecurityUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 用户表 sys_user
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 10000000L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 用户名
     * */
    private String username;
    /**
     * 密码
     * */
    @TableField("password")
    private String password;
    /**
     * 昵称
     * */
    private String nickname;
    /**
     * 邮箱
     * */
    private String email;
    /**
     * 手机号
     * */
    private String phone;
    /**
     * 性别
     * */
    private String sex;
    /**
     * 头像路径
     * */
    private String avatar;
    /**
     * 个人简介
     * */
    private String introduction;
    /**
     * 状态
     * */
    private String status;
    /**
     * 微信 openID
     * */
    private String wxOpenId;

    /**
     * 角色 ID
     * */
    @TableField(exist = false)
    private List<Long> roleIds;

    /**
     * 角色列表
     * */
    @TableField(exist = false)
    private Set<String> roles;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    public boolean isAdmin() {
        return SecurityUtils.isAdmin();
    }
}
