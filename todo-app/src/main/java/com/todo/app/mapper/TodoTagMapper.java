package com.todo.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.todo.common.entity.TodoTag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoTagMapper extends BaseMapper<TodoTag> {
}
