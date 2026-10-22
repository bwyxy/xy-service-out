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
 * 番茄专注记录表 t_todo_focus_session
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_focus_session")
public class TodoFocusSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 专注记录 Id
     * */
    @TableId(value = "session_id", type = IdType.AUTO)
    private Long sessionId;

    /**
     * 用户 Id t_user.u_id
     * */
    private Long userId;

    /**
     * 可关联任务 Id
     * */
    private Long taskId;

    /**
     * 模式 focus/short/long
     * */
    private String mode;

    /**
     * 计划秒数
     * */
    private Integer plannedSeconds;

    /**
     * 实际专注秒数
     * */
    private Integer actualSeconds;

    /**
     * 开始时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 状态 running/finished/aborted
     * */
    private String status;

    /**
     * 客户端幂等 Id
     * */
    private String clientId;

    /**
     * 创建时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
