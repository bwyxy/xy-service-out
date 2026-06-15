package com.todo.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.todo.common.entity.TodoTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TodoTaskMapper extends BaseMapper<TodoTask> {

    /**
     *
     * */
    List<TodoTask> selectTaskByDate(TodoTask todoTask);
}
