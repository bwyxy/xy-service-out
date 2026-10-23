package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoNotifyLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订阅消息下发日志（只写不查，排查「用户说没收到提醒」时直接翻表）
 * */
@Mapper
public interface NotifyLogMapper extends BaseMapper<TodoNotifyLog> {
}
