package com.todo.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.todo.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜单表 sys_menu
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_menu")
public class SysMenu extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单 Id
     * */
    private Long menuId;

    /**
     * 菜单名称
     * */
    private String menuName;

    /**
     * 父菜单 Id
     * */
    private Long parentId;

    /**
     * 显示排序
     * */
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
    private Integer isFrame;

    /**
     * 是否缓存
     * 0-缓存 1-不缓存
     * */
    private Integer isCache;

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
}
