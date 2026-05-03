package com.todo.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.todo.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 系统资源表
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_resource")
public class SysResource extends BaseEntity implements Serializable {

    /**
     * 资源 Id
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 原始文件名
     * */
    private String resourceName;

    /**
     * 存储唯一标识
     * */
    private String resourceKey;

    /**
     * 资源访问 url
     * */
    private String resourceUrl;

    /**
     * 文件大小
     * */
    private Long fileSize;

    /**
     * MIME 类型
     * */
    private String fileType;

    /**
     * 文件后缀
     * */
    private String suffix;

    /**
     * 音视频时长
     * */
    private int duration;
}
