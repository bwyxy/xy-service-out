package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 清单模板表 t_todo_task_template
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_task_template")
public class TodoTaskTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模板 Id
     * */
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    /**
     * 模板编码
     * */
    private String code;

    /**
     * 模板名称
     * */
    private String name;

    /**
     * 图标名
     * */
    private String icon;

    /**
     * 模板描述
     * */
    private String description;

    /**
     * 预置任务数组 JSON：[{title,priority}]
     * */
    private String items;

    /**
     * 排序
     * */
    private Integer sortNo;

    /**
     * 状态 1-启用 0-停用
     * */
    private Integer status;

    /**
     * 创建时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
