package com.xy.system.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.forum.ForumPost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛帖子 Mapper
 */
@Mapper
public interface ForumPostMapper extends BaseMapper<ForumPost> {
}
