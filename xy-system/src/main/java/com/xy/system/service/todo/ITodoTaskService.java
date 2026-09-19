package com.xy.system.service.todo;

import com.xy.common.entity.TodoTask;

import java.time.LocalDateTime;
import java.util.List;

public interface ITodoTaskService {

    /**
     * 根据开始时间结束时间查询待办任务列表
     * */
    List<TodoTask> listPendingTasks(LocalDateTime start, LocalDateTime end);
}
