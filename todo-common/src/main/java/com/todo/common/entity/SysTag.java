package com.todo.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.todo.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 标签表 sys_tag
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_tag")
public class SysTag extends BaseEntity implements Serializable {

    /**
     * 标签 Id
     * */
    @TableId(value = "tag_id", type = IdType.AUTO)
    private Long tagId;

    /**
     * 标签名称
     * */
    private String name;

    /**
     * 颜色
     * */
    private String color;

    /**
     * URL 别名
     * */
    private String slug;

    /**
     * 描述
     * */
    private String description;
}
