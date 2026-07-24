package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 待办任务标签关联表
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("todo_task_tag")
public class TodoTaskTag implements Serializable {

    /**
     * 任务 Id
     * */
    private Long taskId;

    /**
     * 标签 Id
     * */
    private Long tagId;
}
