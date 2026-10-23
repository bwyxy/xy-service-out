package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoFocusSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户端番茄专注 Mapper（表 t_todo_focus_session）
 *
 * <p>类名不带 Todo 前缀，是为了避开 xy-system 里的管理端同名 Mapper——
 * xy-app 的 {@code @MapperScan("com.xy.*.mapper")} 会一并扫到 {@code com.xy.system.mapper}，
 * 同名接口会按简单类名注册出同名 Bean。</p>
 *
 * <p>专注记录没有 deleted 字段（无逻辑删除），增删改查都走 MyBatis-Plus，
 * 今日统计的记录量很小，一次查回内存里聚合即可，不必为它手写聚合 SQL。</p>
 * */
@Mapper
public interface FocusSessionMapper extends BaseMapper<TodoFocusSession> {
}
