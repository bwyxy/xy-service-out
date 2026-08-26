package com.xy.system.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.forum.ForumLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛点赞 Mapper
 */
@Mapper
public interface ForumLikeMapper extends BaseMapper<ForumLike> {
}
