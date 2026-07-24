package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务协作表 todo_task_collaborator
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("todo_task_collaborator")
public class TodoTaskCollaborator extends BaseEntity implements Serializable {

    /**
     * 任务 Id
     * */
    private Long taskId;

    /**
     * 用户 Id
     * */
    private Long userId;

    /**
     * 是否任务管理员
     * */
    private String isAdmin;

    /**
     * 加入时间
     * */
    private LocalDateTime joinedAt;
}
