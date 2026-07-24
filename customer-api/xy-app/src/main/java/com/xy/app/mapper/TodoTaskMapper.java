package com.xy.app.mapper;

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

}
