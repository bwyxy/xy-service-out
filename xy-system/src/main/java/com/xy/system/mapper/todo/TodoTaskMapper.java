package com.xy.system.mapper.todo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.TodoTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TodoTaskMapper extends BaseMapper<TodoTask> {

    /**
     *
     * */
    List<TodoTask> selectTaskByDate(TodoTask todoTask);

    /**
     * 查询任务列表
     * */
    List<TodoTask> selectTaskList(TodoTask todoTask);

    /**
     * 查询任务信息
      */
    TodoTask selectTaskById(@Param("id") Long id);

    /**
     * 批量统计指定父任务ID列表的子任务总数和已完成数
     * 返回 map 包含 parentTaskId, total, completed
     * */
    List<Map<String, Object>> countSubTasksByParentIds(@Param("parentIds") List<Long> parentIds);

    /**
     * 管理后台分页查询任务列表（关联用户名、标签）
     */
    List<TodoTask> selectAdminTaskList(@Param("task") TodoTask todoTask);

    /**
     * 管理后台统计任务总数
     */
    Long countAdminTaskList(@Param("task") TodoTask todoTask);

    /**
     * 根据任务ID查询任务及标签
     */
    TodoTask selectAdminTaskById(@Param("taskId") Long taskId);

    /**
     * 管理后台新增任务
     */
    int insertAdminTask(@Param("task") TodoTask task);

    /**
     * 管理后台修改任务
     */
    int updateAdminTask(@Param("task") TodoTask task);

    /**
     * 管理后台删除任务（逻辑删除）
     */
    int deleteAdminTask(@Param("taskId") Long taskId);

    /**
     * 管理后台查询日程表任务（按时间范围重叠）
     */
    List<TodoTask> selectAdminScheduleTasks(@Param("task") TodoTask todoTask);

}
