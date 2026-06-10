package com.todo.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.todo.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 待办任务表 todo_task
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("todo_task")
public class TodoTask extends BaseEntity implements Serializable {

    /**
     * 任务 Id
     * */
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    /**
     * 用户 Id
     * */
    private Long userId;

    /**
     * 任务标题
     * */
    private String taskTitle;

    /**
     * 描述
     * */
    private String description;

    /**
     * 是否完成
     * 0-未完成 1-已完成
     * */
    private Long completed;

    /**
     * 完成时间
     * */
    private LocalDateTime completionTime;

    /**
     * 紧急程度
     * 1～4
     * */
    private int urgencyLevel;

    /**
     * 开始时间
     * */
    private LocalDateTime startTime;

    /**
     * 截止时间
     * */
    private LocalDateTime dueDate;

    /**
     * 用户信息
     * */
    @TableField(exist = false)
    private SysUser sysUser;
}
