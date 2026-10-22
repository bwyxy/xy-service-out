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
 * 用户偏好设置表 t_todo_user_setting
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_user_setting")
public class TodoUserSetting extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 Id t_user.u_id
     * */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /**
     * 主题 system/light/dark
     * */
    private String theme;

    /**
     * 字号 small/standard/large
     * */
    private String fontSize;

    /**
     * 周起始 0-周日 1-周一
     * */
    private Integer weekStart;

    /**
     * 任务提醒开关
     * */
    private Integer taskRemindOn;

    /**
     * 每日提醒开关
     * */
    private Integer dailyRemindOn;

    /**
     * 每日提醒时间 HH:mm
     * */
    private String dailyRemindTime;

    /**
     * 番茄提醒开关
     * */
    private Integer focusRemindOn;

    /**
     * 免打扰开始 HH:mm
     * */
    private String quietStart;

    /**
     * 免打扰结束 HH:mm
     * */
    private String quietEnd;

    /**
     * 自动同步/备份开关
     * */
    private Integer autoSync;

    /**
     * 上次同步时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncTime;
}
