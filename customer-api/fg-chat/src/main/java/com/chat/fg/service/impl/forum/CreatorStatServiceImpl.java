package com.chat.fg.service.impl.forum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chat.fg.pojo.vo.CreatorStatsVO;
import com.chat.fg.service.forum.ICreatorStatService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.entity.forum.ForumComment;
import com.xy.common.entity.forum.ForumFavorite;
import com.xy.common.entity.forum.ForumFollow;
import com.xy.common.entity.forum.ForumLike;
import com.xy.system.service.forum.IForumCommentService;
import com.xy.system.service.forum.IForumFavoriteService;
import com.xy.system.service.forum.IForumFollowService;
import com.xy.system.service.forum.IForumLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CreatorStatServiceImpl implements ICreatorStatService {

    @Autowired
    private IForumCommentService forumCommentService;

    @Autowired
    private IForumLikeService forumLikeService;

    @Autowired
    private IForumFavoriteService forumFavoriteService;

    @Autowired
    private IForumFollowService forumFollowService;

    public CreatorStatsVO getCreatorStats() {

        Long uId = SecurityUtils.getUId();

        Long commentCount = getCommentCount(uId);
        Long likeCount = getLikeCount(uId);
        Long followerCount = getFollowerCount(uId);
        Long favoriteCount = getFavoriteCount(uId);
        Long followCount = getFollowerCount(uId);

        CreatorStatsVO creatorStatsVO = new CreatorStatsVO();

        creatorStatsVO.setCommentCount(commentCount);
        creatorStatsVO.setLikeCount(likeCount);
        creatorStatsVO.setFavoriteCount(favoriteCount);
        creatorStatsVO.setFollowerCount(followerCount);
        creatorStatsVO.setFollowingCount(followCount);

        return creatorStatsVO;
    }

    /**
     * 获取评论数
     * */
    private Long getCommentCount(Long uId) {
        LambdaQueryWrapper<ForumComment> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ForumComment::getAuthorId, uId);

        return forumCommentService.count(queryWrapper);
    }

    /**
     * 获取点赞数
     * */
    private Long getLikeCount(Long uId) {
        LambdaQueryWrapper<ForumLike> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ForumLike::getUId, uId);

        return forumLikeService.count(queryWrapper);
    }

    /**
     * 获取收藏数
     * */
    private Long getFavoriteCount(Long uId) {
        LambdaQueryWrapper<ForumFavorite> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ForumFavorite::getUId, uId);

        return forumFavoriteService.count(queryWrapper);
    }

    /**
     * 获取用户关注人数
     * */
    private Long getFollowCount(Long uId) {
        LambdaQueryWrapper<ForumFollow> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ForumFollow::getUId, uId);

        return forumFollowService.count(queryWrapper);
    }

    /**
     * 获取用户粉丝数
     * */
    private Long getFollowerCount(Long uId) {
        LambdaQueryWrapper<ForumFollow> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ForumFollow::getTargetId, uId);

        return forumFollowService.count(queryWrapper);
    }
}
