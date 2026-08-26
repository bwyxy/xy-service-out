package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumFavorite;
import com.xy.system.mapper.forum.ForumFavoriteMapper;
import com.xy.system.service.forum.IForumFavoriteService;
import org.springframework.stereotype.Service;

/**
 * 论坛收藏 Service 实现
 */
@Service
public class ForumFavoriteServiceImpl extends ServiceImpl<ForumFavoriteMapper, ForumFavorite>
        implements IForumFavoriteService {
}
