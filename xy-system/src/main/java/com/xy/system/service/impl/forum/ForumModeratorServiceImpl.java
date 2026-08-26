package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumModerator;
import com.xy.system.mapper.forum.ForumModeratorMapper;
import com.xy.system.service.forum.IForumModeratorService;
import org.springframework.stereotype.Service;

/**
 * 版主 Service 实现
 */
@Service
public class ForumModeratorServiceImpl extends ServiceImpl<ForumModeratorMapper, ForumModerator>
        implements IForumModeratorService {
}
