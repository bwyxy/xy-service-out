package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户端待办任务表 t_todo_task
 *
 * <p>注意：与管理端旧表 todo_task 的实体 {@link com.xy.common.entity.TodoTask} 无关，
 * 客户端与管理端两套表结构相互独立。</p>
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_task")
public class TodoTask extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务 Id
     * */
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    /**
     * 所属用户 Id t_user.u_id
     * */
    private Long userId;

    /**
     * 父任务 Id，子任务用
     * */
    private Long parentId;

    /**
     * 任务标题
     * */
    private String taskTitle;

    /**
     * 任务描述
     * */
    private String description;

    /**
     * 是否完成 0-未完成 1-已完成
     * */
    private Long completed;

    /**
     * 完成时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTime;

    /**
     * 紧急程度 1~4，四象限页用
     * */
    private Integer urgencyLevel;

    /**
     * 开始时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 截止时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;

    /**
     * 清单 Id t_todo_tag.tag_id
     * */
    private Long tagId;

    /**
     * 优先级 0-无 1-普通 2-重要
     * */
    private Integer priority;

    /**
     * 重复规则 DAILY / WEEKLY / MONTHLY，空串表示不重复
     * */
    private String repeatRule;

    /**
     * 重复任务：本任务由哪一条「完成后」生成，用于取消完成时回滚
     * */
    private Long repeatSourceId;

    /**
     * 提前提醒分钟数，0-不提醒
     * */
    private Integer remindBefore;

    /**
     * 下次提醒时间（服务端算好）
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime remindAt;

    /**
     * 是否归档 0-否 1-是
     * */
    private Integer archived;

    /**
     * 归档时间（手动归档或完成满一定天数自动归档时写入，取消归档清空）
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime archiveTime;

    /**
     * 客户端幂等 Id
     * */
    private String clientId;

    /**
     * 用户维度同步序号
     * */
    private Long seq;

    /**
     * 创建者
     * */
    private String createBy;

    /**
     * 更新者
     * */
    private String updateBy;

    /**
     * 逻辑删除 0-正常 1-已删除
     * */
    private Integer deleted;

    /**
     * 子任务列表（不落库）
     * */
    @TableField(exist = false)
    private List<TodoTask> tasks;
}
