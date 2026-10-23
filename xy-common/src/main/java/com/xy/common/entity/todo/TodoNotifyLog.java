package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订阅消息下发日志表 t_todo_notify_log
 *
 * <p>成功和失败都记一条，这是排查「用户说没收到提醒」的唯一凭据。
 * 表里只有 send_time，没有 create_time/update_time，所以不继承 BaseEntity。</p>
 * */
@Data
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
     * 订阅消息模板 Id
     * */
    private String templateId;

    /**
     * 点击跳转页
     * */
    private String page;

    /**
     * 微信返回的 errcode，0 表示成功
     * */
    private Integer errcode;

    /**
     * 微信返回的 errmsg
     * */
    private String errmsg;

    /**
     * 下发时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;
}
