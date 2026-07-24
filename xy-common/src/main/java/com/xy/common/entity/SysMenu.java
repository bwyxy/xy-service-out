package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xy.common.core.domain.BaseEntity;
import jakarta.persistence.OrderBy;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单表 sys_menu
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_menu")
public class SysMenu extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 10000000L;

    /**
     * 菜单 Id
     * */
    @TableId(value = "menu_id", type = IdType.AUTO)
    private Long menuId;

    /**
     * 菜单名称
     * */
    private String menuName;

    /**
     * 父菜单 Id
     * */
    @OrderBy(value = "DESC")
    private Long parentId;

    /**
     * 显示排序
     * */
    @OrderBy(value = "DESC")
    private Integer orderNum;

    /**
     * 路由地址
     * */
    private String path;

    /**
     * 组件路径
     * */
    private String component;

    /**
     * 路由参数
     * */
    private String query;

    /**
     * 路由名称
     * */
    private String routeName;

    /**
     * 是否为外链
     * 0-是 1-不是
     * */
    private String isFrame;

    /**
     * 是否缓存
     * 0-缓存 1-不缓存
     * */
    private String isCache;

    /**
     * 菜单类型
     * M-目录 C-菜单 F-按钮
     * */
    private String menuType;

    /**
     * 显示状态
     * 0-显示 1-隐藏
     * */
    private String visible;

    /**
     * 状态
     * 0-正常 1-停用
     * */
    private String status;

    /**
     * 权限标识
     * */
    private String perms;

    /**
     * 菜单图标
     * */
    private String icon;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 子菜单 */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<SysMenu>();
}
