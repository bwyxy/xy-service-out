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
 * 客户端待办清单模板表 t_todo_task_template
 *
 * <p>模板是全局数据，不区分用户；items 存预置任务的 JSON 数组，形如
 * {@code [{"title":"喝一杯温水","priority":1}]}。</p>
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_task_template")
public class TodoTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模板 Id
     * */
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    /**
     * 模板编码（前端套用时传这个，唯一）
     * */
    private String code;

    /**
     * 模板名称，套用后作为新建清单的名称
     * */
    private String name;

    /**
     * 图标名（对应前端 y-icon 的 name）
     * */
    private String icon;

    /**
     * 模板简介
     * */
    private String description;

    /**
     * 分类（生活 / 工作 / 阅读）
     * */
    private String category;

    /**
     * 预置任务 JSON 数组
     * */
    private String items;

    /**
     * 排序
     * */
    private Integer sortNo;

    /**
     * 状态 0-下架 1-上架
     * */
    private Integer status;

    /**
     * 创建时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
