package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoHabit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户端习惯 Mapper（表 t_todo_habit）
 *
 * <p>类名不带 Todo 前缀，是为了避开 xy-system 里的管理端同名 Mapper——
 * xy-app 的 {@code @MapperScan("com.xy.*.mapper")} 会一并扫到 {@code com.xy.system.mapper}，
 * 同名接口会按简单类名注册出同名 Bean。</p>
 *
 * <p>习惯的增删改查都走 MyBatis-Plus，逻辑删除字段 deleted 由全局配置生效，无需手写 SQL。</p>
 * */
@Mapper
public interface HabitMapper extends BaseMapper<TodoHabit> {
}
