package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 内容配置表 sys_config
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_config")
public class SysConfig extends BaseEntity implements Serializable {

    /**
     * 配置 Id
     * */
    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;

    /**
     * 父配置 Id
     * */
    private Long parentId;

    /**
     * 配置名称
     * */
    private String configName;

    /**
     * 配置值
     * */
    private String configValue;

    /**
     * 配置链接
     * */
    private String configLink;

    /**
     * 配置类型
     * */
    private String configType;

    /**
     * 状态
     * 0-隐藏 1-显示
     * */
    private Long status;

    /**
     * 颜色
     * */
    private String color;

    /**
     * 是否删除
     * 0- 存在 1-删除
     * */
    private Integer deleted;
}
