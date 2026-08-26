package com.xy.system.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.forum.ForumComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛评论 Mapper
 */
@Mapper
public interface ForumCommentMapper extends BaseMapper<ForumComment> {
}
