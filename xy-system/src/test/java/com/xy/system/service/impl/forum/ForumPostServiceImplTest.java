package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import com.xy.common.core.redis.RedisCache;
import com.xy.common.entity.forum.ForumLike;
import com.xy.common.entity.forum.ForumPost;
import com.xy.common.enums.forum.PostStatus;
import com.xy.common.exception.ServiceException;
import com.xy.system.mapper.forum.ForumLikeMapper;
import com.xy.system.mapper.forum.ForumPostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

/**
 * 论坛帖子服务单元测试
 * */
@ExtendWith(MockitoExtension.class)
class ForumPostServiceImplTest {

    @Mock
    private ForumPostMapper postMapper;

    @Mock
    private ForumLikeMapper likeMapper;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private ForumPostServiceImpl postService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "baseMapper", postMapper);
        // 手动初始化 MyBatis-Plus lambda 缓存，避免 LambdaUpdateWrapper 报错
        Configuration configuration = new Configuration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, ForumPost.class);
        TableInfoHelper.initTableInfo(builderAssistant, ForumLike.class);
    }

    @Test
    void getForumPosts_shouldReturnPageResult() {
        ForumPost query = new ForumPost();
        query.setBoardId(1L);
        query.setStatus(PostStatus.published.getCode());
        query.setPageNum(1);
        query.setPageSize(10);

        ForumPost post = createPost(1L, "测试帖子", 2);
        Page<ForumPost> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(post));

        when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<ForumPost> result = postService.getForumPosts(query);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("测试帖子", result.getList().get(0).getTitle());
    }

    @Test
    void getForumPostDetail_shouldReturnPost() {
        ForumPost post = createPost(1L, "测试帖子", PostStatus.published.getCode());
        when(postMapper.selectById(1L)).thenReturn(post);

        ForumPost result = postService.getForumPostDetail(1L);

        assertNotNull(result);
        assertEquals("测试帖子", result.getTitle());
    }

    @Test
    void getForumPostDetail_shouldThrowWhenPostNotFound() {
        when(postMapper.selectById(1L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> postService.getForumPostDetail(1L));
        assertEquals("帖子不存在", exception.getMessage());
    }

    @Test
    void getForumPostDetail_shouldThrowWhenNotPublished() {
        ForumPost post = createPost(1L, "未发布帖子", PostStatus.underReview.getCode());
        when(postMapper.selectById(1L)).thenReturn(post);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> postService.getForumPostDetail(1L));
        assertEquals("帖子未发布或已被下架", exception.getMessage());
    }

    @Test
    void likePost_shouldAddLikeWhenNoRecord() {
        ForumPost post = createPost(1L, "测试帖子", PostStatus.published.getCode());
        when(postMapper.selectById(1L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(likeMapper.insert(any(ForumLike.class))).thenReturn(1);
        when(postMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        boolean result = postService.likePost(1L, 100L);

        assertTrue(result);
        verify(likeMapper, times(1)).insert(any(ForumLike.class));
        verify(postMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
        verify(redisCache, times(1)).setCacheObject(anyString(), eq(true), anyInt(), any());
    }

    @Test
    void likePost_shouldCancelLikeWhenAlreadyLiked() {
        ForumPost post = createPost(1L, "测试帖子", PostStatus.published.getCode());
        ForumLike likeRecord = new ForumLike();
        likeRecord.setId(1L);
        likeRecord.setStatus(1);

        when(postMapper.selectById(1L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(likeRecord);
        when(likeMapper.updateById(any(ForumLike.class))).thenReturn(1);
        when(postMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        boolean result = postService.likePost(1L, 100L);

        assertFalse(result);
        assertEquals(0, likeRecord.getStatus());
        verify(likeMapper, times(1)).updateById(any(ForumLike.class));
    }

    @Test
    void likePost_shouldThrowWhenPostIdNull() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> postService.likePost(null, 100L));
        assertEquals("帖子ID或用户ID不能为空", exception.getMessage());
    }

    @Test
    void incrementViewCount_shouldIncreaseWhenFirstView() {
        when(redisCache.getCacheObject(anyString())).thenReturn(null);
        doNothing().when(redisCache).setCacheObject(anyString(), eq(true), anyInt(), any());
        when(postMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> postService.incrementViewCount(1L, 100L));

        verify(redisCache, times(1)).setCacheObject(anyString(), eq(true), anyInt(), any());
        verify(postMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void incrementViewCount_shouldNotIncreaseWhenRecentlyViewed() {
        when(redisCache.getCacheObject(anyString())).thenReturn(true);

        assertDoesNotThrow(() -> postService.incrementViewCount(1L, 100L));

        verify(postMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void incrementViewCount_shouldThrowWhenPostIdNull() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> postService.incrementViewCount(null, 100L));
        assertEquals("帖子ID不能为空", exception.getMessage());
    }

    @Test
    void hasLiked_shouldReturnTrueWhenCached() {
        when(redisCache.getCacheObject(anyString())).thenReturn(true);

        assertTrue(postService.hasLiked(1L, 100L));
        verify(likeMapper, never()).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    void hasLiked_shouldReturnFalseWhenNoRecord() {
        when(redisCache.getCacheObject(anyString())).thenReturn(null);
        when(likeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertFalse(postService.hasLiked(1L, 100L));
    }

    @Test
    void auditPost_shouldPublishPost() {
        ForumPost post = createPost(1L, "待审核帖子", PostStatus.underReview.getCode());
        when(postMapper.selectById(1L)).thenReturn(post);
        when(postMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        String result = postService.auditPost(1L, PostStatus.published.getCode());

        assertEquals("帖子已发布", result);
    }

    @Test
    void auditPost_shouldRefusePost() {
        ForumPost post = createPost(1L, "待审核帖子", PostStatus.underReview.getCode());
        when(postMapper.selectById(1L)).thenReturn(post);
        when(postMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        String result = postService.auditPost(1L, PostStatus.refuse.getCode());

        assertEquals("帖子已拒绝发布", result);
    }

    @Test
    void auditPost_shouldThrowWhenStatusInvalid() {
        ForumPost post = createPost(1L, "待审核帖子", PostStatus.underReview.getCode());
        when(postMapper.selectById(1L)).thenReturn(post);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> postService.auditPost(1L, 999));
        assertEquals("传入帖子状态错误", exception.getMessage());
    }

    @Test
    void topPost_shouldToggleTopStatus() {
        ForumPost post = createPost(1L, "测试帖子", PostStatus.published.getCode());
        post.setIsTop(0);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(postMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> postService.topPost(1L));
        verify(postMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void essencePost_shouldToggleEssenceStatus() {
        ForumPost post = createPost(1L, "测试帖子", PostStatus.published.getCode());
        post.setIsEssence(0);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(postMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> postService.essencePost(1L));
        verify(postMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
    }

    private ForumPost createPost(Long postId, String title, int status) {
        ForumPost post = new ForumPost();
        post.setPostId(postId);
        post.setTitle(title);
        post.setStatus(status);
        post.setAuthorId(100L);
        post.setBoardId(1L);
        post.setViewCount(0L);
        post.setLikeCount(0L);
        post.setIsTop(0);
        post.setIsEssence(0);
        return post;
    }
}
