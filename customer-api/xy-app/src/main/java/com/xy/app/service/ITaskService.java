package com.xy.app.service;

import com.xy.common.core.domain.PageResult;
import com.xy.common.dto.ToDoTaskDTO;
import com.xy.common.entity.TodoTask;

public interface ITaskService {

    /**
     * 根据传入时间 查询当天任务
     * */
    public PageResult<TodoTask> selectTaskByDate(TodoTask xyTask);

    /**
     * 查询任务列表
     * */
    public PageResult<TodoTask> selectTaskList(TodoTask xyTask);

    /**
     * 根据任务 Id查询任务信息
     * */
    public TodoTask selectTaskById(Long id);

    /**
     * 创建任务
     * */
    public void createTask(ToDoTaskDTO TodoTaskDTO);

    /**
     * 编辑任务
     * */
    public void updateTask(ToDoTaskDTO TodoTaskDTO);

    /**
     * 设置任务完成状态
     * */
    public void setTaskCompleted(TodoTask xyTask);

    /**
     * 删除任务
     * */
    public void deleteTaskByTaskId(Long taskId);
}
