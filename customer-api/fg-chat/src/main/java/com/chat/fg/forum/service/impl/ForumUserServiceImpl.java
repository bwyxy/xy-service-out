package com.chat.fg.forum.service.impl;

import com.chat.fg.forum.service.IForumUserService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 论坛用户 服务实现
 */
@Service
public class ForumUserServiceImpl implements IForumUserService {

    @Override
    public Map<String, Object> profile(Long uId) {
        // TODO 后续实现用户论坛主页
        return new HashMap<>();
    }
}
