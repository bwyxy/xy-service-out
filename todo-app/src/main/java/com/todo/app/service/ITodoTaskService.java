package com.todo.app.service;

import com.todo.common.core.domain.PageResult;
import com.todo.common.dto.ToDoTaskDTO;
import com.todo.common.entity.TodoTask;

public interface ITodoTaskService {

    /**
     * 根据传入时间 查询当天任务
     * */
    public PageResult<TodoTask> selectTaskByDate(TodoTask todoTask);

    /**
     * 根据任务 Id查询任务信息
     * */
    public TodoTask selectTaskById(Long id);

    /**
     * 创建任务
     * */
    public void createTask(ToDoTaskDTO toDoTaskDTO);

    /**
     * 编辑任务
     * */
    public void updateTask(ToDoTaskDTO toDoTaskDTO);

    /**
     * 设置任务完成状态
     * */
    public void setTaskCompleted(TodoTask todoTask);

    /**
     * 删除任务
     * */
    public void deleteTaskByTaskId(Long taskId);
}
