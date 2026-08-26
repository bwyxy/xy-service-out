package com.chat.fg.forum.service;

import java.util.Map;

/**
 * 论坛用户 服务接口
 */
public interface IForumUserService {

    /**
     * 用户论坛主页
     */
    Map<String, Object> profile(Long uId);
}
