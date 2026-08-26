package com.xy.system.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.forum.ForumFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛收藏 Mapper
 */
@Mapper
public interface ForumFavoriteMapper extends BaseMapper<ForumFavorite> {
}
