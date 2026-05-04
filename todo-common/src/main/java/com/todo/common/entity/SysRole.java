package com.todo.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.todo.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色表 sys_role
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_role")
public class SysRole extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 10000000L;

    /**
     * 角色 Id
     * */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称
     * */
    private String roleName;

    /**
     * 角色权限字符串
     * */
    private String roleKey;

    /**
     * 显示顺序
     * */
    private Integer roleSort;

    /**
     * 数据范围
     * 1-全部 2-自定义
     * */
    private String dataScope;

    /**
     * 状态
     * */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}