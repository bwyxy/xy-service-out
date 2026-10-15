package com.xy.system.service.impl.blog;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.dto.blog.DispatchStatQueryDTO;
import com.xy.common.entity.blog.BlogDispatchArticleStat;
import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.entity.blog.BlogDispatchTask;
import com.xy.common.enums.DispatchStatusEnum;
import com.xy.common.vo.blog.DispatchStatSummaryVO;
import com.xy.system.dispatch.adapter.DispatchAdapterRegistry;
import com.xy.system.dispatch.adapter.DispatchPlatformAdapter;
import com.xy.system.dispatch.adapter.model.StatResult;
import com.xy.system.mapper.blog.BlogDispatchArticleStatMapper;
import com.xy.system.mapper.blog.BlogDispatchTaskMapper;
import com.xy.system.service.blog.IBlogDispatchPlatformService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 分发运营数据服务单元测试
 * 覆盖：分发成功登记记录、定时采集回写准确性、已有记录更新、平台不支持采集与采集异常时的容错
 * */
@ExtendWith(MockitoExtension.class)
class BlogDispatchStatServiceImplTest {

    @Mock
    private BlogDispatchArticleStatMapper statMapper;

    @Mock
    private BlogDispatchTaskMapper taskMapper;

    @Mock
    private IBlogDispatchPlatformService platformService;

    @Mock
    private DispatchAdapterRegistry adapterRegistry;

    @Mock
    private DispatchPlatformAdapter adapter;

    private BlogDispatchStatServiceImpl statService;

    @BeforeEach
    void setUp() {
        statService = spy(new BlogDispatchStatServiceImpl(platformService, adapterRegistry, taskMapper));
        ReflectionTestUtils.setField(statService, "baseMapper", statMapper);

        Configuration configuration = new Configuration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, BlogDispatchArticleStat.class);
        TableInfoHelper.initTableInfo(builderAssistant, BlogDispatchTask.class);
    }

    private BlogDispatchTask successTask(Long id, String platformArticleId) {
        BlogDispatchTask task = new BlogDispatchTask();
        task.setId(id);
        task.setArticleId(100L + id);
        task.setPlatformCode("juejin");
        task.setPlatformName("掘金");
        task.setStatus(DispatchStatusEnum.SUCCESS.getCode());
        task.setPlatformArticleId(platformArticleId);
        task.setResultUrl("https://juejin.cn/post/" + platformArticleId);
        return task;
    }

    private BlogDispatchPlatform platform(int enabled) {
        BlogDispatchPlatform platform = new BlogDispatchPlatform();
        platform.setPlatformCode("juejin");
        platform.setPlatformName("掘金");
        platform.setEnabled(enabled);
        platform.setRequestTimeoutSeconds(15);
        return platform;
    }

    private StatResult statResult(String platformArticleId, int view, int like, int comment, int collect) {
        StatResult result = new StatResult();
        result.setPlatformArticleId(platformArticleId);
        result.setViewCount(view);
        result.setLikeCount(like);
        result.setCommentCount(comment);
        result.setCollectCount(collect);
        return result;
    }

    @Test
    void refreshStats_shouldPersistPlatformMetricsAccurately() {
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(
                List.of(successTask(1L, "7001"), successTask(2L, "7002")));
        when(platformService.getByCode("juejin")).thenReturn(platform(1));
        when(platformService.decryptCredentials(any())).thenReturn(Map.of("cookie", "session=abc"));
        when(adapterRegistry.find("juejin")).thenReturn(adapter);
        when(adapter.supportsStatFetch()).thenReturn(true);
        when(adapter.fetchStats(any())).thenReturn(List.of(
                statResult("7001", 100, 20, 5, 3),
                statResult("7002", 88, 6, 2, 1)));
        when(statMapper.selectOne(any(), eq(false))).thenReturn(null);
        when(statMapper.insert(any(BlogDispatchArticleStat.class))).thenReturn(1);

        int updated = statService.refreshStats();

        assertEquals(2, updated);

        ArgumentCaptor<BlogDispatchArticleStat> captor = ArgumentCaptor.forClass(BlogDispatchArticleStat.class);
        verify(statMapper, times(2)).insert(captor.capture());
        List<BlogDispatchArticleStat> inserted = captor.getAllValues();

        BlogDispatchArticleStat first = inserted.get(0);
        assertEquals(1L, first.getTaskId());
        assertEquals(101L, first.getArticleId());
        assertEquals("juejin", first.getPlatformCode());
        assertEquals("7001", first.getPlatformArticleId());
        assertEquals("https://juejin.cn/post/7001", first.getResultUrl());
        assertEquals(100, first.getViewCount());
        assertEquals(20, first.getLikeCount());
        assertEquals(5, first.getCommentCount());
        assertEquals(3, first.getCollectCount());

        BlogDispatchArticleStat second = inserted.get(1);
        assertEquals(88, second.getViewCount());
        assertEquals(1, second.getCollectCount());
    }

    @Test
    void refreshStats_shouldUpdateExistingRecord() {
        BlogDispatchArticleStat existing = new BlogDispatchArticleStat();
        existing.setId(66L);
        existing.setTaskId(1L);

        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(successTask(1L, "7001")));
        when(platformService.getByCode("juejin")).thenReturn(platform(1));
        when(platformService.decryptCredentials(any())).thenReturn(Map.of("cookie", "session=abc"));
        when(adapterRegistry.find("juejin")).thenReturn(adapter);
        when(adapter.supportsStatFetch()).thenReturn(true);
        when(adapter.fetchStats(any())).thenReturn(List.of(statResult("7001", 100, 20, 5, 3)));
        when(statMapper.selectOne(any(), eq(false))).thenReturn(existing);
        when(statMapper.update(any(), any())).thenReturn(1);

        int updated = statService.refreshStats();

        assertEquals(1, updated);
        verify(statMapper, never()).insert(any(BlogDispatchArticleStat.class));

        ArgumentCaptor<Wrapper<BlogDispatchArticleStat>> captor = captureUpdateWrapper();
        LambdaUpdateWrapper<BlogDispatchArticleStat> wrapper =
                (LambdaUpdateWrapper<BlogDispatchArticleStat>) captor.getValue();
        String sqlSet = wrapper.getSqlSet().replace("_", "").toLowerCase();
        assertTrue(sqlSet.contains("viewcount"), "应更新浏览量: " + sqlSet);
        assertTrue(sqlSet.contains("likecount"), "应更新点赞数: " + sqlSet);
        assertTrue(sqlSet.contains("commentcount"), "应更新评论数: " + sqlSet);
        assertTrue(sqlSet.contains("collectcount"), "应更新收藏数: " + sqlSet);
        assertTrue(wrapper.getParamNameValuePairs().containsValue(100), "浏览量数值应准确回写");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(3), "收藏数数值应准确回写");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Wrapper<BlogDispatchArticleStat>> captureUpdateWrapper() {
        ArgumentCaptor<Wrapper<BlogDispatchArticleStat>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(statMapper).update(isNull(), captor.capture());
        return captor;
    }

    @Test
    void refreshStats_shouldReturnZero_whenNoSuccessTask() {
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        assertEquals(0, statService.refreshStats());
        verifyNoInteractions(adapterRegistry);
    }

    @Test
    void refreshStats_shouldReturnZero_whenPlatformDisabled() {
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(successTask(1L, "7001")));
        when(platformService.getByCode("juejin")).thenReturn(platform(0));

        assertEquals(0, statService.refreshStats());
        verify(adapterRegistry, never()).find(anyString());
    }

    @Test
    void refreshStats_shouldReturnZero_whenAdapterNotSupportStatFetch() {
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(successTask(1L, "7001")));
        when(platformService.getByCode("juejin")).thenReturn(platform(1));
        when(adapterRegistry.find("juejin")).thenReturn(adapter);
        when(adapter.supportsStatFetch()).thenReturn(false);

        assertEquals(0, statService.refreshStats());
        verify(adapter, never()).fetchStats(any());
    }

    @Test
    void refreshStats_shouldReturnZero_whenCredentialMissing() {
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(successTask(1L, "7001")));
        when(platformService.getByCode("juejin")).thenReturn(platform(1));
        when(adapterRegistry.find("juejin")).thenReturn(adapter);
        when(adapter.supportsStatFetch()).thenReturn(true);
        when(platformService.decryptCredentials(any())).thenReturn(Collections.emptyMap());

        assertEquals(0, statService.refreshStats());
        verify(adapter, never()).fetchStats(any());
    }

    @Test
    void refreshStats_shouldSwallowPlatformException() {
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(successTask(1L, "7001")));
        when(platformService.getByCode("juejin")).thenReturn(platform(1));
        when(platformService.decryptCredentials(any())).thenReturn(Map.of("cookie", "session=abc"));
        when(adapterRegistry.find("juejin")).thenReturn(adapter);
        when(adapter.supportsStatFetch()).thenReturn(true);
        when(adapter.fetchStats(any())).thenThrow(new RuntimeException("平台接口超时"));

        assertEquals(0, statService.refreshStats());
    }

    @Test
    void refreshStats_shouldIgnoreResultOfUnknownArticle() {
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(successTask(1L, "7001")));
        when(platformService.getByCode("juejin")).thenReturn(platform(1));
        when(platformService.decryptCredentials(any())).thenReturn(Map.of("cookie", "session=abc"));
        when(adapterRegistry.find("juejin")).thenReturn(adapter);
        when(adapter.supportsStatFetch()).thenReturn(true);
        when(adapter.fetchStats(any())).thenReturn(List.of(statResult("9999", 1, 1, 1, 1)));

        assertEquals(0, statService.refreshStats());
        verify(statMapper, never()).insert(any(BlogDispatchArticleStat.class));
    }

    @Test
    void recordPublishSuccess_shouldInsertRecordWithZeroMetrics() {
        when(statMapper.selectOne(any(), eq(false))).thenReturn(null);
        when(statMapper.insert(any(BlogDispatchArticleStat.class))).thenReturn(1);

        statService.recordPublishSuccess(5L, 100L, "juejin", "7001", "https://juejin.cn/post/7001");

        ArgumentCaptor<BlogDispatchArticleStat> captor = ArgumentCaptor.forClass(BlogDispatchArticleStat.class);
        verify(statMapper).insert(captor.capture());
        BlogDispatchArticleStat saved = captor.getValue();
        assertEquals(5L, saved.getTaskId());
        assertEquals(100L, saved.getArticleId());
        assertEquals("juejin", saved.getPlatformCode());
        assertEquals("7001", saved.getPlatformArticleId());
        assertEquals("https://juejin.cn/post/7001", saved.getResultUrl());
        assertEquals(0, saved.getViewCount());
        assertEquals(0, saved.getLikeCount());
        assertEquals(0, saved.getCommentCount());
        assertEquals(0, saved.getCollectCount());
        assertNotNull(saved.getStatTime());
    }

    @Test
    void recordPublishSuccess_shouldUpdateResultUrl_whenRecordExists() {
        BlogDispatchArticleStat existing = new BlogDispatchArticleStat();
        existing.setId(66L);
        existing.setTaskId(5L);
        when(statMapper.selectOne(any(), eq(false))).thenReturn(existing);
        when(statMapper.update(any(), any())).thenReturn(1);

        statService.recordPublishSuccess(5L, 100L, "juejin", "7001", "https://juejin.cn/post/7001");

        verify(statMapper, never()).insert(any(BlogDispatchArticleStat.class));
        ArgumentCaptor<Wrapper<BlogDispatchArticleStat>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(statMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<BlogDispatchArticleStat> wrapper =
                (LambdaUpdateWrapper<BlogDispatchArticleStat>) captor.getValue();
        assertTrue(wrapper.getSqlSet().replace("_", "").toLowerCase().contains("resulturl"), "应回写平台访问链接");
        assertTrue(wrapper.getParamNameValuePairs().containsValue("https://juejin.cn/post/7001"));
    }

    @Test
    void recordPublishSuccess_shouldIgnoreBlankTaskId() {
        statService.recordPublishSuccess(null, 100L, "juejin", "7001", "https://juejin.cn/post/7001");

        verifyNoInteractions(statMapper);
    }

    @Test
    void selectStatPage_shouldReturnPageResult() {
        DispatchStatQueryDTO query = new DispatchStatQueryDTO();
        query.setKeyword("分发");
        query.setPageNum(1);
        query.setPageSize(10);

        BlogDispatchArticleStat record = new BlogDispatchArticleStat();
        record.setId(1L);
        record.setArticleTitle("分发测试");
        record.setViewCount(50);
        Page<BlogDispatchArticleStat> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(record));
        when(statMapper.selectStatPage(any(Page.class), any(DispatchStatQueryDTO.class))).thenReturn(page);

        PageResult<BlogDispatchArticleStat> result = statService.selectStatPage(query);

        assertEquals(1, result.getList().size());
        assertEquals(50, result.getList().get(0).getViewCount());
        assertEquals("分发测试", result.getList().get(0).getArticleTitle());
    }

    @Test
    void selectPlatformSummary_shouldReturnGroupedMetrics() {
        DispatchStatSummaryVO summary = new DispatchStatSummaryVO();
        summary.setPlatformCode("juejin");
        summary.setPlatformName("掘金");
        summary.setArticleCount(2L);
        summary.setViewCount(188L);
        when(statMapper.selectPlatformSummary("juejin")).thenReturn(List.of(summary));

        List<DispatchStatSummaryVO> result = statService.selectPlatformSummary("juejin");

        assertEquals(1, result.size());
        assertEquals(188L, result.get(0).getViewCount());
    }
}
