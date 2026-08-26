package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumLike;
import com.xy.system.mapper.forum.ForumLikeMapper;
import com.xy.system.service.forum.IForumLikeService;
import org.springframework.stereotype.Service;

/**
 * 论坛点赞 Service 实现
 */
@Service
public class ForumLikeServiceImpl extends ServiceImpl<ForumLikeMapper, ForumLike>
        implements IForumLikeService {
}
