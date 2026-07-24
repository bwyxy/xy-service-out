package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分类表 sys_category
 * */

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_category")
public class SysCategory extends BaseEntity implements Serializable {

    /**
     * 分类 Id
     * */
    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;

    /**
     * 分类名称
     * */
    private String name;

    /**
     * URL 别名
     * */
    private String slug;

    /**
     * 描述
     * */
    private String description;
}
