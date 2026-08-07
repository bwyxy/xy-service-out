package com.chat.fg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知表 数据层
 * */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
