package com.xy.common.entity.blog;

import com.baomidou.mybatisplus.annotation.*;
import com.xy.common.core.domain.BaseEntity;
import com.xy.common.entity.SysMenu;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单配置表 blog_menu
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_menu")
public class BlogMenu extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long parentId;

    private Integer orderNum;

    private String path;

    private String routeName;

    private Integer isFrame;

    private Integer status;

    private String icon;

    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 子菜单 */
    @TableField(exist = false)
    private List<BlogMenu> children = new ArrayList<>();
}
