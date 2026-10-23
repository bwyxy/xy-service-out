package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户端待办清单 Mapper（表 t_todo_tag）
 * */
@Mapper
public interface TagMapper extends BaseMapper<TodoTag> {
}
