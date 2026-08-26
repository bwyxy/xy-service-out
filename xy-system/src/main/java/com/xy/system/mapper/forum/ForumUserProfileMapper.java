package com.xy.system.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.forum.ForumUserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛用户扩展资料 Mapper
 */
@Mapper
public interface ForumUserProfileMapper extends BaseMapper<ForumUserProfile> {
}
