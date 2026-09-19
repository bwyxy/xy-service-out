package com.xy.system.service.todo;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.TodoTask;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ITodoTaskAdminService {

    /**
     * 分页查询待办任务列表
     */
    PageResult<TodoTask> selectTaskList(TodoTask task);

    /**
     * 根据任务ID查询详情
     */
    TodoTask selectTaskById(Long taskId);

    /**
     * 新增待办任务
     */
    Long addTask(TodoTask task);

    /**
     * 修改待办任务
     */
    boolean updateTask(TodoTask task);

    /**
     * 删除待办任务
     */
    boolean deleteTask(Long taskId);

    /**
     * 查询日程表数据：按时间范围返回任务列表
     */
    List<TodoTask> listScheduleTasks(LocalDateTime start, LocalDateTime end);

    /**
     * 查询仪表盘统计：任务总数、已完成、未完成、逾期数量
     */
    Map<String, Long> getDashboardStatistics();
}
