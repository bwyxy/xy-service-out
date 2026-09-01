package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.redis.RedisCache;
import com.xy.common.dto.forum.CreatePostDTO;
import com.xy.common.entity.User;
import com.xy.common.entity.forum.ForumBoard;
import com.xy.common.entity.forum.ForumFavorite;
import com.xy.common.entity.forum.ForumLike;
import com.xy.common.entity.forum.ForumPost;
import com.xy.common.entity.forum.ForumPostTopic;
import com.xy.common.entity.forum.ForumTopic;
import com.xy.common.enums.YesNoEnum;
import com.xy.common.enums.forum.PostStatus;
import com.xy.common.exception.ServiceException;
import com.xy.common.ip.AddressUtils;
import com.xy.common.ip.IpUtils;
import com.xy.common.utils.StringUtils;
import com.xy.common.vo.forum.ForumPostVO;
import com.xy.system.mapper.UserMapper;
import com.xy.system.mapper.forum.ForumBoardMapper;
import com.xy.system.mapper.forum.ForumFavoriteMapper;
import com.xy.system.mapper.forum.ForumLikeMapper;
import com.xy.system.mapper.forum.ForumPostMapper;
import com.xy.system.mapper.forum.ForumPostTopicMapper;
import com.xy.system.mapper.forum.ForumTopicMapper;
import com.xy.system.service.forum.IForumPostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 论坛帖子 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForumPostServiceImpl extends ServiceImpl<ForumPostMapper, ForumPost>
        implements IForumPostService {

    /**
     * 点赞目标类型：帖子
     * */
    private static final int LIKE_TARGET_POST = 1;

    /**
     * 缓存前缀：用户点赞状态
     * */
    private static final String LIKE_CACHE_KEY_PREFIX = "forum:like:post:";

    /**
     * 缓存前缀：用户收藏状态
     * */
    private static final String FAVORITE_CACHE_KEY_PREFIX = "forum:favorite:post:";

    /**
     * 缓存前缀：用户浏览记录
     * */
    private static final String VIEW_CACHE_KEY_PREFIX = "forum:view:post:";

    private final ForumLikeMapper likeMapper;

    private final ForumFavoriteMapper favoriteMapper;

    private final UserMapper userMapper;

    private final ForumBoardMapper boardMapper;

    private final ForumTopicMapper topicMapper;

    private final ForumPostTopicMapper postTopicMapper;

    private final RedisCache redisCache;

    /**
     * 发布帖子
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(CreatePostDTO dto, Long uId, HttpServletRequest request) {
        if (dto == null || uId == null) {
            throw new ServiceException("参数错误");
        }

        String ip = IpUtils.getIpAddr(request);
        String ipAddr = AddressUtils.getRealAddressByIP(ip);
        LocalDateTime now = LocalDateTime.now();

        ForumPost post = new ForumPost();
        post.setAuthorId(uId);
        post.setTitle(dto.getTitle().trim());
        post.setBoardId(4L);
        post.setContent(dto.getContent());
        post.setSummary(generateSummary(dto.getContent()));
        post.setContentType(dto.getContentType());
        post.setCoverImage(dto.getCoverImage());
        post.setVisibility(dto.getVisibility());
        post.setStatus(PostStatus.underReview.getCode());
        post.setIsTop(0);
        post.setIsEssence(0);
        post.setViewCount(0L);
        post.setLikeCount(0L);
        post.setCommentCount(0L);
        post.setFavoriteCount(0L);
        post.setShareCount(0L);
        post.setPublishedTime(now);
        post.setCreateTime(now);
        post.setUpdateTime(now);
        post.setIp(ip);
        post.setIpAddr(ipAddr);

        baseMapper.insert(post);
        Long postId = post.getPostId();

        // 处理话题
        handleTopics(postId, dto.getTopicNames(), now);

        log.info("用户 {} 发布帖子 {}", uId, postId);
        return postId;
    }

    /**
     * 生成摘要
     * */
    private String generateSummary(String content) {
        if (StringUtils.isBlank(content)) {
            return "";
        }
        // 去除 HTML 标签
        String text = content.replaceAll("<[^>]+>", "");
        text = text.replaceAll("&\\w+;", " ");
        text = text.trim();
        if (text.length() <= 200) {
            return text;
        }
        return text.substring(0, 200);
    }

    /**
     * 处理话题关联
     * */
    private void handleTopics(Long postId, List<String> topicNames, LocalDateTime now) {
        if (postId == null || topicNames == null || topicNames.isEmpty()) {
            return;
        }

        for (String topicName : topicNames) {
            if (StringUtils.isBlank(topicName)) {
                continue;
            }
            String name = topicName.trim();

            LambdaQueryWrapper<ForumTopic> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ForumTopic::getName, name);
            ForumTopic topic = topicMapper.selectOne(wrapper);

            if (topic == null) {
                topic = new ForumTopic();
                topic.setName(name);
                topic.setSlug(generateSlug(name));
                topic.setStatus(1);
                topic.setPostCount(0L);
                topic.setFollowCount(0L);
                topic.setCreateTime(now);
                topic.setUpdateTime(now);
                topicMapper.insert(topic);
            }

            ForumPostTopic postTopic = new ForumPostTopic();
            postTopic.setPostId(postId);
            postTopic.setTopicId(topic.getTopicId());
            postTopicMapper.insert(postTopic);
        }
    }

    /**
     * 生成话题 URL 别名
     * */
    private String generateSlug(String name) {
        String slug = name.toLowerCase()
                .replaceAll("[^\\w\\u4e00-\\u9fa5]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (StringUtils.isBlank(slug)) {
            slug = "topic";
        }
        return slug;
    }

    /**
     * 查询帖子列表（支持分页、排序、条件筛选）
     * */
    @Override
    public PageResult<ForumPostVO> getForumPosts(ForumPost forumPost, Long currentUid) {
        long start = System.currentTimeMillis();
        LambdaQueryWrapper<ForumPost> queryWrapper = new LambdaQueryWrapper<>();

        if (forumPost.getAuthorId() != null) {
            queryWrapper.eq(ForumPost::getAuthorId, forumPost.getAuthorId());
        }
        if (forumPost.getBoardId() != null && forumPost.getBoardId() > 0) {
            queryWrapper.eq(ForumPost::getBoardId, forumPost.getBoardId());
        }
        if (forumPost.getStatus() != null) {
            queryWrapper.eq(ForumPost::getStatus, forumPost.getStatus());
        }
        if (StringUtils.isNotBlank(forumPost.getTitle())) {
            queryWrapper.like(ForumPost::getTitle, forumPost.getTitle());
        }
        if (forumPost.getIsTop() != null) {
            queryWrapper.eq(ForumPost::getIsTop, forumPost.getIsTop());
        }
        if (forumPost.getIsEssence() != null) {
            queryWrapper.eq(ForumPost::getIsEssence, forumPost.getIsEssence());
        }

        // 默认排序：置顶 > 发布时间倒序
        queryWrapper.orderByDesc(ForumPost::getIsTop)
                .orderByDesc(ForumPost::getPublishedTime);

        Page<ForumPost> page = this.page(
                new Page<>(forumPost.getPageNum(), forumPost.getPageSize()),
                queryWrapper
        );

        List<ForumPostVO> voList = enrichPostList(page.getRecords(), currentUid);

        long cost = System.currentTimeMillis() - start;
        if (cost > 300) {
            log.warn("查询帖子列表耗时超过300ms，cost={}ms", cost);
        }

        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 查询帖子详情
     * */
    @Override
    public ForumPostVO getForumPostDetail(Long postId, Long currentUid) {
        if (postId == null) {
            throw new ServiceException("帖子ID不能为空");
        }

        ForumPost forumPost = getById(postId);
        if (forumPost == null) {
            throw new ServiceException("帖子不存在");
        }

        // 未发布状态不允许查看
        if (forumPost.getStatus() == null || forumPost.getStatus() != PostStatus.published.getCode()) {
            throw new ServiceException("帖子未发布或已被下架");
        }

        return enrichPost(forumPost, currentUid);
    }

    /**
     * 填充帖子列表的扩展信息
     * */
    private List<ForumPostVO> enrichPostList(List<ForumPost> posts, Long currentUid) {
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询作者信息
        Set<Long> authorIds = posts.stream()
                .map(ForumPost::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = batchGetUsers(authorIds);

        // 批量查询当前用户点赞状态
        Set<Long> postIds = posts.stream()
                .map(ForumPost::getPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Boolean> likeMap = batchGetLikeStatus(postIds, currentUid);

        // 批量查询当前用户收藏状态
        Map<Long, Boolean> favoriteMap = batchGetFavoriteStatus(postIds, currentUid);

        return posts.stream()
                .map(post -> {
                    ForumPostVO vo = convertToVO(post);
                    User user = userMap.get(post.getAuthorId());
                    if (user != null) {
                        vo.setNickname(user.getNickname());
                        vo.setAvatar(user.getAvatar());
                    }
                    vo.setLiked(likeMap.getOrDefault(post.getPostId(), false));
                    vo.setFavorited(favoriteMap.getOrDefault(post.getPostId(), false));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 填充单个帖子的扩展信息
     * */
    private ForumPostVO enrichPost(ForumPost post, Long currentUid) {
        ForumPostVO vo = convertToVO(post);

        User user = userMapper.selectById(post.getAuthorId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        if (currentUid != null) {
            vo.setLiked(hasLiked(post.getPostId(), currentUid));
            vo.setFavorited(hasFavorited(post.getPostId(), currentUid));
        } else {
            vo.setLiked(false);
            vo.setFavorited(false);
        }

        return vo;
    }

    /**
     * 批量查询作者信息
     * */
    private Map<Long, User> batchGetUsers(Set<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getUId, authorIds);
        return userMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(User::getUId, Function.identity(), (a, b) -> a));
    }

    /**
     * 批量查询点赞状态
     * */
    private Map<Long, Boolean> batchGetLikeStatus(Set<Long> postIds, Long currentUid) {
        if (postIds == null || postIds.isEmpty() || currentUid == null) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<ForumLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumLike::getTargetType, LIKE_TARGET_POST)
                .eq(ForumLike::getUId, currentUid)
                .in(ForumLike::getTargetId, postIds)
                .eq(ForumLike::getStatus, 1);

        return likeMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(ForumLike::getTargetId, like -> true, (a, b) -> a));
    }

    /**
     * 批量查询收藏状态
     * */
    private Map<Long, Boolean> batchGetFavoriteStatus(Set<Long> postIds, Long currentUid) {
        if (postIds == null || postIds.isEmpty() || currentUid == null) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<ForumFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumFavorite::getUId, currentUid)
                .in(ForumFavorite::getPostId, postIds);

        return favoriteMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(ForumFavorite::getPostId, favorite -> true, (a, b) -> a));
    }

    /**
     * 查询用户是否已点赞
     * */
    @Override
    public boolean hasLiked(Long postId, Long uId) {
        if (postId == null || uId == null) {
            return false;
        }

        String likeKey = LIKE_CACHE_KEY_PREFIX + postId + ":" + uId;
        Boolean cached = redisCache.getCacheObject(likeKey);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<ForumLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumLike::getTargetType, LIKE_TARGET_POST)
                .eq(ForumLike::getTargetId, postId)
                .eq(ForumLike::getUId, uId)
                .eq(ForumLike::getStatus, 1);
        boolean liked = likeMapper.selectCount(wrapper) > 0;

        redisCache.setCacheObject(likeKey, liked, 5, TimeUnit.MINUTES);
        return liked;
    }

    /**
     * 查询用户是否已收藏
     * */
    @Override
    public boolean hasFavorited(Long postId, Long uId) {
        if (postId == null || uId == null) {
            return false;
        }

        String favoriteKey = FAVORITE_CACHE_KEY_PREFIX + postId + ":" + uId;
        Boolean cached = redisCache.getCacheObject(favoriteKey);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<ForumFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumFavorite::getPostId, postId)
                .eq(ForumFavorite::getUId, uId);
        boolean favorited = favoriteMapper.selectCount(wrapper) > 0;

        redisCache.setCacheObject(favoriteKey, favorited, 5, TimeUnit.MINUTES);
        return favorited;
    }

    /**
     * 点赞 / 取消点赞
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likePost(Long postId, Long uId) {
        if (postId == null || uId == null) {
            throw new ServiceException("帖子ID或用户ID不能为空");
        }

        ForumPost forumPost = validatePost(postId);

        // 查询当前点赞记录
        LambdaQueryWrapper<ForumLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumLike::getTargetType, LIKE_TARGET_POST)
                .eq(ForumLike::getTargetId, postId)
                .eq(ForumLike::getUId, uId);
        ForumLike likeRecord = likeMapper.selectOne(wrapper);

        boolean isLiked;
        LocalDateTime now = LocalDateTime.now();

        if (likeRecord == null) {
            // 新增点赞
            ForumLike newLike = new ForumLike();
            newLike.setTargetType(LIKE_TARGET_POST);
            newLike.setTargetId(postId);
            newLike.setUId(uId);
            newLike.setStatus(1);
            newLike.setCreateTime(now);
            newLike.setUpdateTime(now);
            likeMapper.insert(newLike);

            // 帖子点赞数 +1
            incrementLikeCount(postId, 1);
            isLiked = true;
            log.info("用户 {} 点赞帖子 {}", uId, postId);
        } else if (likeRecord.getStatus() == null || likeRecord.getStatus() == 0) {
            // 恢复点赞
            likeRecord.setStatus(1);
            likeRecord.setUpdateTime(now);
            likeMapper.updateById(likeRecord);

            incrementLikeCount(postId, 1);
            isLiked = true;
            log.info("用户 {} 重新点赞帖子 {}", uId, postId);
        } else {
            // 取消点赞
            likeRecord.setStatus(0);
            likeRecord.setUpdateTime(now);
            likeMapper.updateById(likeRecord);

            incrementLikeCount(postId, -1);
            isLiked = false;
            log.info("用户 {} 取消点赞帖子 {}", uId, postId);
        }

        // 刷新缓存中的点赞状态
        String likeKey = LIKE_CACHE_KEY_PREFIX + postId + ":" + uId;
        redisCache.setCacheObject(likeKey, isLiked, 5, TimeUnit.MINUTES);

        return isLiked;
    }

    /**
     * 增加帖子浏览量（带防重复）
     * */
    @Override
    public void incrementViewCount(Long postId, Long uId) {
        if (postId == null) {
            throw new ServiceException("帖子ID不能为空");
        }

        String viewKey = VIEW_CACHE_KEY_PREFIX + postId + ":" + (uId != null ? uId : "guest");
        Boolean hasViewed = redisCache.getCacheObject(viewKey);

        if (Boolean.TRUE.equals(hasViewed)) {
            // 短时间内已浏览过，不再重复统计
            return;
        }

        // 标记已浏览，设置30分钟过期
        redisCache.setCacheObject(viewKey, true, 30, TimeUnit.MINUTES);

        // 浏览量 +1
        LambdaUpdateWrapper<ForumPost> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ForumPost::getPostId, postId);
        updateWrapper.setSql("view_count = view_count + 1");
        updateWrapper.set(ForumPost::getUpdateTime, LocalDateTime.now());
        update(updateWrapper);

        log.info("帖子 {} 浏览量 +1", postId);
    }

    /**
     * 审核通过或拒绝
     * */
    public String auditPost(Long postId, int status) {
        validatePost(postId);

        if (status != PostStatus.published.getCode() && status != PostStatus.refuse.getCode()) {
            throw new ServiceException("传入帖子状态错误");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ForumPost> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(ForumPost::getPostId, postId);
        updateWrapper.set(ForumPost::getStatus, status);
        updateWrapper.set(ForumPost::getUpdateTime, now);
        if (status == PostStatus.published.getCode()) {
            updateWrapper.set(ForumPost::getPublishedTime, now);
        }

        update(updateWrapper);

        log.info("帖子 {} 审核状态变更为 {}", postId, status);

        if (status == PostStatus.refuse.getCode()) {
            return "帖子已拒绝发布";
        } else {
            return "帖子已发布";
        }
    }

    /**
     * 帖子置顶或取消
     * */
    public void topPost(Long postId) {
        ForumPost forumPost = validatePost(postId);

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ForumPost> updateWrapper = new LambdaUpdateWrapper<>();
        // 是否置顶
        int isTop = forumPost.getIsTop();
        updateWrapper.eq(ForumPost::getPostId, postId);
        if (isTop == YesNoEnum.YES.getCode()) {
            updateWrapper.set(ForumPost::getIsTop, YesNoEnum.NO.getCode());
        } else {
            updateWrapper.set(ForumPost::getIsTop, YesNoEnum.YES.getCode());
        }
        updateWrapper.set(ForumPost::getUpdateTime, now);
        update(updateWrapper);

        log.info("帖子 {} 置顶状态切换为 {}", postId, isTop == YesNoEnum.YES.getCode() ? 0 : 1);
    }

    /**
     * 帖子加精或取消
     * */
    public void essencePost(Long postId) {
        ForumPost forumPost = validatePost(postId);

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ForumPost> updateWrapper = new LambdaUpdateWrapper<>();
        // 是否是精品
        int isEssence = forumPost.getIsEssence();
        updateWrapper.eq(ForumPost::getPostId, postId);
        if (isEssence == YesNoEnum.YES.getCode()) {
            updateWrapper.set(ForumPost::getIsEssence, YesNoEnum.NO.getCode());
        } else {
            updateWrapper.set(ForumPost::getIsEssence, YesNoEnum.YES.getCode());
        }
        updateWrapper.set(ForumPost::getUpdateTime, now);
        update(updateWrapper);

        log.info("帖子 {} 精华状态切换为 {}", postId, isEssence == YesNoEnum.YES.getCode() ? 0 : 1);
    }

    /**
     * 验证信息传入帖子 ID正确性并返回帖子信息
     * */
    private ForumPost validatePost(Long postId) {
        if (postId == null) {
            throw new ServiceException("帖子ID不能为空");
        }

        ForumPost forumPost = getForumPost(postId);

        if (forumPost == null) {
            throw new ServiceException("查无帖子信息");
        }

        return forumPost;
    }

    /**
     * 获取贴子信息
     * */
    private ForumPost getForumPost(Long postId) {
        return getById(postId);
    }

    /**
     * 更新帖子点赞数
     * */
    private void incrementLikeCount(Long postId, int delta) {
        LambdaUpdateWrapper<ForumPost> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ForumPost::getPostId, postId);
        updateWrapper.setSql("like_count = like_count + " + delta);
        updateWrapper.set(ForumPost::getUpdateTime, LocalDateTime.now());
        update(updateWrapper);
    }

    /**
     * 实体转换为 VO
     * */
    private ForumPostVO convertToVO(ForumPost post) {
        ForumPostVO vo = new ForumPostVO();
        vo.setPostId(post.getPostId());
        vo.setAuthorId(post.getAuthorId());
        vo.setBoardId(post.getBoardId());
        vo.setTitle(post.getTitle());
        vo.setSummary(post.getSummary());
        vo.setContent(post.getContent());
        vo.setContentType(post.getContentType());
        vo.setCoverImage(post.getCoverImage());
        vo.setStatus(post.getStatus());
        vo.setVisibility(post.getVisibility());
        vo.setIsTop(post.getIsTop());
        vo.setIsEssence(post.getIsEssence());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setFavoriteCount(post.getFavoriteCount());
        vo.setPublishedTime(post.getPublishedTime());
        vo.setCreateTime(post.getCreateTime());
        return vo;
    }
}
