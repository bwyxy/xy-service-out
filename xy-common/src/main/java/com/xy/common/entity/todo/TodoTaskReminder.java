package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务提醒表 t_todo_task_reminder（一个任务可有多个提醒点）
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_task_reminder")
public class TodoTaskReminder extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 提醒 Id
     * */
    @TableId(value = "reminder_id", type = IdType.AUTO)
    private Long reminderId;

    /**
     * 任务 Id
     * */
    private Long taskId;

    /**
     * 用户 Id t_user.u_id
     * */
    private Long userId;

    /**
     * 提醒时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime remindAt;

    /**
     * 订阅消息模板 Id
     * */
    private String templateId;

    /**
     * 状态 pending/sending/sent/failed/canceled
     * */
    private String status;

    /**
     * 已重试次数
     * */
    private Integer retryCount;

    /**
     * 实际下发时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;

    /**
     * 失败原因（errcode+errmsg）
     * */
    private String failReason;
}
