package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumUserProfile;
import com.xy.system.mapper.forum.ForumUserProfileMapper;
import com.xy.system.service.forum.IForumUserProfileService;
import org.springframework.stereotype.Service;

/**
 * 论坛用户扩展资料 Service 实现
 */
@Service
public class ForumUserProfileServiceImpl extends ServiceImpl<ForumUserProfileMapper, ForumUserProfile>
        implements IForumUserProfileService {
}
