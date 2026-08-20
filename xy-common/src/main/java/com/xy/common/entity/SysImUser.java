package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 悟空 IM系统账号 sys_im_user
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_im_user")
public class SysImUser extends BaseEntity {

    /**
     * id
     * */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 头像
     * */
    private String avatar;

    /**
     * 系统账号名称
     * */
    private String nickname;

    /**
     * 账号标识
     * */
    private String userKey;

    /**
     * 悟空 IM系统标识
     * */
    private String wkImKey;

    /**
     * 是否绑定悟空 IM标识
     * */
    @TableField(exist = false)
    private boolean isBindWkImKey;
}
