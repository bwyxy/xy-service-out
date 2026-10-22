package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xy.common.core.domain.BaseEntity;
import com.xy.common.utils.SecurityUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 客户端用户表 t_user
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 10000000L;

    @Serial
    private static final List<Long> uIds = List.of(10000001L, 10000002L);

    /**
     * 用户 Id
     * */
    @TableId(value = "u_id", type = IdType.AUTO)
    private Long uId;

    /**
     * 昵称
     * */
    private String nickname;

    /**
     * 密码
     * */
    @TableField("password")
    private String password;

    /**
     * 头像
     * */
    private String avatar;

    /**
     * 手机号
     * */
    private String phone;

    /**
     * 邮箱
     * */
    private String email;

    /**
     * 状态
     * 0-正常 1-禁用
     * */
    private String status;

    /**
     * 性别
     * */
    private String sex;

    /**
     * 微信小程序 open_id
     * */
    private String wxOpenId;

    /**
     * 是否绑定聊天应用
     * */
    private String isBindChat;

    /**
     * 是否超级管理员
     *
     * <p>Jackson 会把 isAdmin() 当作 admin 属性序列化，进而调用 SecurityUtils 读取后台登录态；
     * 而待办登录态的主体是 TodoUserDetails，强转 LoginUser 会抛异常，因此这里不参与序列化。</p>
     * */
    @JsonIgnore
    public boolean isAdmin() {
        return SecurityUtils.isAdmin();
    }

}
