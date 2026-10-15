package com.xy.system.service.impl.blog;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xy.common.dto.blog.DispatchRequestDTO;
import com.xy.common.entity.SysArticle;
import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.entity.blog.BlogDispatchTask;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.common.enums.DispatchStatusEnum;
import com.xy.common.exception.ServiceException;
import com.xy.common.vo.blog.DispatchSubmitVO;
import com.xy.system.dispatch.DispatchTaskExecutor;
import com.xy.system.mapper.blog.BlogDispatchTaskMapper;
import com.xy.system.service.ISysArticleService;
import com.xy.system.service.blog.IBlogDispatchPlatformService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 分发任务服务单元测试
 * 覆盖：单篇分发、多篇多平台批量分发、已分发去重、平台未配置/未启用/无权限跳过、重试与失败计数
 * */
@ExtendWith(MockitoExtension.class)
class BlogDispatchTaskServiceImplTest {

    @Mock
    private BlogDispatchTaskMapper taskMapper;

    @Mock
    private IBlogDispatchPlatformService platformService;

    @Mock
    private ISysArticleService articleService;

    @Mock
    private ObjectProvider<DispatchTaskExecutor> executorProvider;

    @Mock
    private DispatchTaskExecutor executor;

    private BlogDispatchTaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        taskService = spy(new BlogDispatchTaskServiceImpl(platformService, articleService, executorProvider));
        ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);

        Configuration configuration = new Configuration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, BlogDispatchTask.class);

        lenient().when(executorProvider.getObject()).thenReturn(executor);
        // saveBatch 落库时回填自增 Id，模拟真实入库行为
        lenient().doAnswer(invocation -> {
            Collection<BlogDispatchTask> tasks = invocation.getArgument(0);
            long id = 1L;
            for (BlogDispatchTask task : tasks) {
                task.setId(id++);
            }
            return true;
        }).when(taskService).saveBatch(anyCollection());
    }

    private BlogDispatchPlatform platform(String code, String name, int enabled, int maxRetry) {
        BlogDispatchPlatform platform = new BlogDispatchPlatform();
        platform.setId(1L);
        platform.setPlatformCode(code);
        platform.setPlatformName(name);
        platform.setEnabled(enabled);
        platform.setMaxRetry(maxRetry);
        return platform;
    }

    private SysArticle article(Long id, String title) {
        SysArticle article = new SysArticle();
        article.setArticleId(id);
        article.setTitle(title);
        article.setContent("<p>正文</p>");
        return article;
    }

    private DispatchRequestDTO request(List<Long> articleIds, List<String> platformCodes, Boolean ignoreDispatched) {
        DispatchRequestDTO dto = new DispatchRequestDTO();
        dto.setArticleIds(articleIds);
        dto.setPlatformCodes(platformCodes);
        dto.setIgnoreDispatched(ignoreDispatched);
        return dto;
    }

    @Test
    void submitDispatch_shouldCreateOneTask_forSingleArticleSinglePlatform() {
        when(platformService.getByCode("juejin")).thenReturn(platform("juejin", "掘金", 1, 3));
        when(platformService.canUsePlatform("juejin")).thenReturn(true);
        when(articleService.selectSysArticleById(1L)).thenReturn(article(1L, "单篇分发"));
        when(taskMapper.selectCount(any())).thenReturn(0L);

        DispatchSubmitVO result = taskService.submitDispatch(
                request(List.of(1L), List.of("juejin"), true));

        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(1, result.getTaskIds().size());
        assertNotNull(result.getBatchNo());

        ArgumentCaptor<Collection<BlogDispatchTask>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(taskService).saveBatch(captor.capture());
        BlogDispatchTask saved = captor.getValue().iterator().next();
        assertEquals(1L, saved.getArticleId());
        assertEquals("单篇分发", saved.getArticleTitle());
        assertEquals("juejin", saved.getPlatformCode());
        assertEquals("掘金", saved.getPlatformName());
        assertEquals(DispatchStatusEnum.PENDING.getCode(), saved.getStatus());
        assertEquals(0, saved.getRetryCount());
        assertEquals(3, saved.getMaxRetry());
        verify(executor).execute(1L);
    }

    @Test
    void submitDispatch_shouldCreateTasks_forBatchArticlesAndPlatforms() {
        when(platformService.getByCode("juejin")).thenReturn(platform("juejin", "掘金", 1, 3));
        when(platformService.getByCode("csdn")).thenReturn(platform("csdn", "CSDN", 1, 2));
        when(platformService.canUsePlatform(anyString())).thenReturn(true);
        when(articleService.selectSysArticleById(1L)).thenReturn(article(1L, "文章一"));
        when(articleService.selectSysArticleById(2L)).thenReturn(article(2L, "文章二"));
        when(taskMapper.selectCount(any())).thenReturn(0L);

        DispatchSubmitVO result = taskService.submitDispatch(
                request(List.of(1L, 2L), List.of("juejin", "csdn"), true));

        assertEquals(4, result.getCreatedCount());
        assertEquals(4, result.getTaskIds().size());
        verify(executor, times(4)).execute(anyLong());
    }

    @Test
    void submitDispatch_shouldSkipDispatchedArticle_whenIgnoreDispatched() {
        when(platformService.getByCode("juejin")).thenReturn(platform("juejin", "掘金", 1, 3));
        when(platformService.canUsePlatform("juejin")).thenReturn(true);
        when(articleService.selectSysArticleById(1L)).thenReturn(article(1L, "已分发文章"));
        // 已存在成功分发记录
        when(taskMapper.selectCount(any())).thenReturn(1L);

        DispatchSubmitVO result = taskService.submitDispatch(
                request(List.of(1L), List.of("juejin"), true));

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getSkippedCount());
        assertTrue(result.getSkippedReasons().get(0).contains("已成功分发"));
        verify(taskService, never()).saveBatch(anyCollection());
        verify(executor, never()).execute(anyLong());
    }

    @Test
    void submitDispatch_shouldNotSkipDispatchedArticle_whenIgnoreDispatchedDisabled() {
        when(platformService.getByCode("juejin")).thenReturn(platform("juejin", "掘金", 1, 3));
        when(platformService.canUsePlatform("juejin")).thenReturn(true);
        when(articleService.selectSysArticleById(1L)).thenReturn(article(1L, "已分发文章"));

        DispatchSubmitVO result = taskService.submitDispatch(
                request(List.of(1L), List.of("juejin"), false));

        assertEquals(1, result.getCreatedCount());
        verify(taskMapper, never()).selectCount(any());
    }

    @Test
    void submitDispatch_shouldSkip_whenPlatformNotConfigured() {
        when(platformService.getByCode("zhihu")).thenReturn(null);

        DispatchSubmitVO result = taskService.submitDispatch(
                request(List.of(1L), List.of("zhihu"), true));

        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getSkippedCount());
        assertTrue(result.getSkippedReasons().get(0).contains("尚未配置"));
    }

    @Test
    void submitDispatch_shouldSkip_whenPlatformDisabled() {
        when(platformService.getByCode("juejin")).thenReturn(platform("juejin", "掘金", 0, 3));

        DispatchSubmitVO result = taskService.submitDispatch(
                request(List.of(1L), List.of("juejin"), true));

        assertEquals(0, result.getCreatedCount());
        assertTrue(result.getSkippedReasons().get(0).contains("未启用"));
    }

    @Test
    void submitDispatch_shouldSkip_whenNoPlatformPermission() {
        when(platformService.getByCode("juejin")).thenReturn(platform("juejin", "掘金", 1, 3));
        when(platformService.canUsePlatform("juejin")).thenReturn(false);

        DispatchSubmitVO result = taskService.submitDispatch(
                request(List.of(1L), List.of("juejin"), true));

        assertEquals(0, result.getCreatedCount());
        assertTrue(result.getSkippedReasons().get(0).contains("无权使用"));
    }

    @Test
    void submitDispatch_shouldSkip_whenArticleNotExists() {
        when(platformService.getByCode("juejin")).thenReturn(platform("juejin", "掘金", 1, 3));
        when(platformService.canUsePlatform("juejin")).thenReturn(true);
        when(articleService.selectSysArticleById(99L)).thenReturn(null);

        DispatchSubmitVO result = taskService.submitDispatch(
                request(List.of(99L), List.of("juejin"), true));

        assertEquals(0, result.getCreatedCount());
        assertTrue(result.getSkippedReasons().get(0).contains("不存在"));
    }

    @Test
    void submitDispatch_shouldThrow_whenArticleIdsEmpty() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> taskService.submitDispatch(request(List.of(), List.of("juejin"), true)));
        assertEquals("请至少选择一篇待分发的文章", exception.getMessage());
    }

    @Test
    void submitDispatch_shouldThrow_whenPlatformCodesEmpty() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> taskService.submitDispatch(request(List.of(1L), List.of(), true)));
        assertEquals("请至少选择一个目标平台", exception.getMessage());
    }

    @Test
    void retryTasks_shouldResetStatusAndSubmitAgain() {
        BlogDispatchTask task = new BlogDispatchTask();
        task.setId(9L);
        task.setStatus(DispatchStatusEnum.FAILED.getCode());
        doReturn(task).when(taskService).getById(9L);
        when(taskMapper.update(any(), any())).thenReturn(1);

        taskService.retryTasks(List.of(9L));

        verify(taskMapper).update(isNull(), any());
        verify(executor).execute(9L);
    }

    @Test
    void retryTasks_shouldSkipRunningTask() {
        BlogDispatchTask task = new BlogDispatchTask();
        task.setId(9L);
        task.setStatus(DispatchStatusEnum.RUNNING.getCode());
        doReturn(task).when(taskService).getById(9L);

        taskService.retryTasks(List.of(9L));

        verify(taskMapper, never()).update(any(), any());
        verify(executor, never()).execute(anyLong());
    }

    @Test
    void markRunning_shouldReturnFalse_whenTaskAlreadyRunning() {
        when(taskMapper.update(any(), any())).thenReturn(0);

        assertFalse(taskService.markRunning(1L));
    }

    @Test
    void markFailed_shouldIncreaseRetryCount() {
        when(taskMapper.update(any(), any())).thenReturn(1);

        taskService.markFailed(1L, DispatchErrorTypeEnum.RATE_LIMITED, "触发限流", 1200L);

        verify(taskMapper).update(isNull(), any());
    }

    @Test
    void selectTaskPage_shouldReturnPageResult() {
        BlogDispatchTask query = new BlogDispatchTask();
        query.setPlatformCode("juejin");
        query.setStatus(DispatchStatusEnum.FAILED.getCode());
        query.setPageNum(1);
        query.setPageSize(15);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<BlogDispatchTask> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 15, 1);
        BlogDispatchTask failed = new BlogDispatchTask();
        failed.setId(3L);
        failed.setArticleTitle("失败任务");
        failed.setPlatformName("掘金");
        failed.setStatus(DispatchStatusEnum.FAILED.getCode());
        failed.setErrorType(DispatchErrorTypeEnum.CREDENTIAL_EXPIRED.getCode());
        failed.setRetryCount(1);
        failed.setMaxRetry(3);
        page.setRecords(List.of(failed));
        doReturn(page).when(taskService).page(any(), any());

        var result = taskService.selectTaskPage(query);

        assertEquals(1, result.getList().size());
        var vo = result.getList().get(0);
        assertEquals("分发失败", vo.getStatusDesc());
        assertTrue(vo.getRetryable());
        assertTrue(vo.getNeedManualFix());
    }

    @Test
    void selectRetryableTaskIds_shouldFilterNonRetryableErrors() {
        BlogDispatchTask retryable = new BlogDispatchTask();
        retryable.setId(1L);
        retryable.setStatus(DispatchStatusEnum.FAILED.getCode());
        retryable.setErrorType(DispatchErrorTypeEnum.NETWORK_ERROR.getCode());
        retryable.setRetryCount(1);
        retryable.setMaxRetry(3);

        BlogDispatchTask manualFix = new BlogDispatchTask();
        manualFix.setId(2L);
        manualFix.setStatus(DispatchStatusEnum.FAILED.getCode());
        manualFix.setErrorType(DispatchErrorTypeEnum.CREDENTIAL_INVALID.getCode());
        manualFix.setRetryCount(1);
        manualFix.setMaxRetry(3);

        doReturn(List.of(retryable, manualFix)).when(taskService)
                .list(ArgumentMatchers.<Wrapper<BlogDispatchTask>>any());

        assertEquals(List.of(1L), taskService.selectRetryableTaskIds(10));
    }
}
