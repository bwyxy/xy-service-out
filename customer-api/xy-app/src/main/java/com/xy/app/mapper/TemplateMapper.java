package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户端清单模板 Mapper（表 t_todo_task_template）
 *
 * <p>模板是全局数据，无用户维度，只按 status / sort_no 查询。</p>
 * */
@Mapper
public interface TemplateMapper extends BaseMapper<TodoTemplate> {
}
