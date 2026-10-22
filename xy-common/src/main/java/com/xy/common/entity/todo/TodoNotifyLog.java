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
 * 订阅消息下发日志表 t_todo_notify_log
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_notify_log")
public class TodoNotifyLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志 Id
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户 Id t_user.u_id
     * */
    private Long userId;

    /**
     * 任务 Id
     * */
    private Long taskId;

    /**
     * 提醒 Id
     * */
    private Long reminderId;

    /**
     * 模板 Id
     * */
    private String templateId;

    /**
     * 点击跳转页
     * */
    private String page;

    /**
     * 微信错误码
     * */
    private Integer errcode;

    /**
     * 微信错误信息
     * */
    private String errmsg;

    /**
     * 下发时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;
}
