package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统内容项配置表
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_content_item")
public class SysContentItem extends BaseEntity implements Serializable {

    /**
     * 内容 Id
     * */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    @TableId(value = "content_id", type = IdType.AUTO)
    private Long contentId;

    /**
     * 类型
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String type;

    /**
     * 标题
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String title;

    /**
     * 内容
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String content;

    /**
     * 描述
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String description;

    /**
     * 链接
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String linkUrl;

    /**
     * 封面图
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String coverImage;

    /**
     * 技术栈
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String techStack;

    /**
     * 图片集(多个图片使用;隔开)
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String images;

    /**
     * 开始时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private LocalDateTime startDate;

    /**
     * 结束时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private LocalDateTime endDate;

    /**
     * 排序
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private int sortOrder;

    /**
     * 状态 1-显示 0-隐藏
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private int status;

    /**
     * 拓展字段
     * */
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String extraData;

    @TableLogic
    private Integer deleted;
}
