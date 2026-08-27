package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.forum.ForumPost;
import com.xy.common.enums.forum.PostStatus;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.forum.ForumPostMapper;
import com.xy.system.service.forum.IForumPostService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 论坛帖子 Service 实现
 */
@Service
public class ForumPostServiceImpl extends ServiceImpl<ForumPostMapper, ForumPost>
        implements IForumPostService {

    /**
     * 查询论坛列表
     * */
    public PageResult<ForumPost> getForumPosts(ForumPost forumPost) {
        LambdaQueryWrapper<ForumPost> queryWrapper = new LambdaQueryWrapper<>();

        if (forumPost.getAuthorId() != null) {
            queryWrapper.eq(ForumPost::getAuthorId, forumPost.getAuthorId());
        }
        if (forumPost.getStatus() != null) {
            queryWrapper.eq(ForumPost::getStatus, forumPost.getStatus());
        }
        if (forumPost.getPublishedTime() != null) {
            queryWrapper.eq(ForumPost::getPublishedTime, forumPost.getPublishedTime());
        }

        Page<ForumPost> page = this.page(new Page<>(forumPost.getPageNum(), forumPost.getPageSize()),  queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 审核通过或拒绝
     * */
    public String auditPost(Long postId, int status) {

        ForumPost forumPost = getForumPost(postId);

        if (forumPost == null) {
            throw new ServiceException("查无帖子信息");
        }

        if (status != PostStatus.published.getCode() || status != PostStatus.refuse.getCode()) {
            throw new ServiceException("传入帖子状态错误");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ForumPost> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(ForumPost::getPostId, postId);
        updateWrapper.set(ForumPost::getStatus, status);
        updateWrapper.set(ForumPost::getUpdateTime, now);

        update(updateWrapper);

        if (status == PostStatus.refuse.getCode()) {
            return "帖子已拒绝发布";
        } else {
            return "帖子已发布";
        }
    }

    /**
     * 获取贴子信息
     * */
    private ForumPost getForumPost(Long postId) {
        LambdaQueryWrapper<ForumPost> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ForumPost::getPostId, postId);

        return this.getOne(queryWrapper);
    }
}
