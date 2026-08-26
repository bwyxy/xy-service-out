package com.xy.system.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.forum.ForumPostTopic;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子话题关联 Mapper
 */
@Mapper
public interface ForumPostTopicMapper extends BaseMapper<ForumPostTopic> {
}
