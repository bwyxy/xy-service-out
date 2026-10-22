package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户端任务-清单关联表 t_todo_task_tag
 *
 * <p>前端目前是单选清单，本表保留结构暂不启用。</p>
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_task_tag")
public class TodoTaskTag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务 Id
     * */
    private Long taskId;

    /**
     * 清单 Id
     * */
    private Long tagId;
}
