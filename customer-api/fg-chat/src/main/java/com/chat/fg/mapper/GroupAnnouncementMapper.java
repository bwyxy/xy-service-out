package com.chat.fg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.chat.GroupAnnouncement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群公告 数据层
 * */
@Mapper
public interface GroupAnnouncementMapper extends BaseMapper<GroupAnnouncement> {
}
