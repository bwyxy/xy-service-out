package com.xy.system.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.forum.ForumTopic;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛话题 Mapper
 */
@Mapper
public interface ForumTopicMapper extends BaseMapper<ForumTopic> {
}
