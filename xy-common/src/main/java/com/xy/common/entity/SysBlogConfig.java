package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 博客系统配置表 sys_blog_config
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_blog_config")
public class SysBlogConfig extends BaseEntity implements Serializable {

    /**
     * 配置 Id
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置标识
     * */
    private String configKey;

    /**
     * 配置值
     * */
    private String configValue;

    /**
     * 描述
     * */
    private String description;
}
