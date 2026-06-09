package com.todo.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.todo.common.entity.TodoTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoTaskMapper extends BaseMapper<TodoTask> {
}
