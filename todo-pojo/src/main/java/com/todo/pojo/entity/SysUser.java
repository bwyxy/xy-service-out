package com.todo.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.todo.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户表 sys_user
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     * */
    private String username;
    /**
     * 密码
     * */
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
}
