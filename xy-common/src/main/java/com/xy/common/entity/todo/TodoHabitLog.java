package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 习惯打卡记录表 t_todo_habit_log
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_habit_log")
public class TodoHabitLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录 Id
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 习惯 Id
     * */
    private Long habitId;

    /**
     * 用户 Id t_user.u_id
     * */
    private Long userId;

    /**
     * 打卡日期
     * */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate logDate;

    /**
     * 状态 1-已打卡
     * */
    private Integer status;

    /**
     * 创建时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
