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
 * 习惯表 t_todo_habit
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_habit")
public class TodoHabit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 习惯 Id
     * */
    @TableId(value = "habit_id", type = IdType.AUTO)
    private Long habitId;

    /**
     * 用户 Id t_user.u_id
     * */
    private Long userId;

    /**
     * 习惯名称
     * */
    private String name;

    /**
     * 图标名
     * */
    private String icon;

    /**
     * 主题色
     * */
    private String color;

    /**
     * 频率类型 daily/weekly
     * */
    private String freqType;

    /**
     * weekly 时的星期集合，如 1,3,5
     * */
    private String freqDays;

    /**
     * 目标天数（0 不限）
     * */
    private Integer targetDays;

    /**
     * 排序
     * */
    private Integer sortNo;

    /**
     * 状态 1-进行中 0-已结束
     * */
    private Integer status;

    /**
     * 逻辑删除 0-正常 1-已删除
     * */
    private Integer deleted;

    /**
     * 创建时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
