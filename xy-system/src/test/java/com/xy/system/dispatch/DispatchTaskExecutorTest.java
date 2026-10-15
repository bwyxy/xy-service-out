package com.xy.system.dispatch;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xy.common.entity.SysArticle;
import com.xy.common.entity.SysArticleTag;
import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.entity.blog.BlogDispatchTask;
import com.xy.common.enums.DispatchContentFormat;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.common.enums.DispatchStatusEnum;
import com.xy.system.dispatch.adapter.DispatchAdapterRegistry;
import com.xy.system.dispatch.adapter.DispatchPlatformAdapter;
import com.xy.system.dispatch.adapter.model.PublishContext;
import com.xy.system.dispatch.adapter.model.PublishResult;
import com.xy.system.dispatch.content.ContentAdaptService;
import com.xy.system.dispatch.content.ImageMigrationResult;
import com.xy.system.dispatch.content.ImageMigrationService;
import com.xy.system.mapper.SysArticleTagMapper;
import com.xy.system.mapper.SysTagMapper;
import com.xy.system.mapper.blog.BlogDispatchTaskMapper;
import com.xy.system.service.ISysArticleService;
import com.xy.system.service.blog.IBlogDispatchPlatformService;
import com.xy.system.service.blog.IBlogDispatchStatService;
import com.xy.system.service.blog.IBlogDispatchTaskService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 分发任务执行器单元测试
 * 覆盖：单平台分发成功并回传平台链接、适配器失败归一化、内容适配异常、平台未启用、任务被抢占等异常场景
 * */
@ExtendWith(MockitoExtension.class)
class DispatchTaskExecutorTest {

    @Mock
    private BlogDispatchTaskMapper taskMapper;

    @Mock
    private IBlogDispatchTaskService taskService;

    @Mock
    private IBlogDispatchPlatformService platformService;

    @Mock
    private IBlogDispatchStatService statService;

    @Mock
    private ISysArticleService articleService;

    @Mock
    private SysArticleTagMapper articleTagMapper;

    @Mock
    private SysTagMapper tagMapper;

    @Mock
    private DispatchAdapterRegistry adapterRegistry;

    @Mock
    private DispatchPlatformAdapter adapter;

    @Mock
    private ImageMigrationService imageMigrationService;

    private DispatchTaskExecutor executor;

    private static final String HTML = "<h1>标题</h1><p>正文内容</p>";

    @BeforeEach
    void setUp() {
        executor = new DispatchTaskExecutor(taskMapper, taskService, platformService, statService,
                articleService, articleTagMapper, tagMapper, adapterRegistry,
                new ContentAdaptService(), imageMigrationService);
        ReflectionTestUtils.setField(executor, "articleUrlPrefix", "https://blog.example.com");

        Configuration configuration = new Configuration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, SysArticleTag.class);
    }

    private BlogDispatchTask task(Long id) {
        BlogDispatchTask task = new BlogDispatchTask();
        task.setId(id);
        task.setArticleId(100L);
        task.setArticleTitle("测试文章");
        task.setPlatformCode("juejin");
        task.setPlatformName("掘金");
        task.setStatus(DispatchStatusEnum.PENDING.getCode());
        task.setRetryCount(0);
        task.setMaxRetry(3);
        return task;
    }

    private BlogDispatchPlatform platform(int enabled) {
        BlogDispatchPlatform platform = new BlogDispatchPlatform();
        platform.setPlatformCode("juejin");
        platform.setPlatformName("掘金");
        platform.setEnabled(enabled);
        platform.setConfigJson("{\"category_id\":\"6809637773935378440\"}");
        platform.setTitleMaxLength(100);
        platform.setSummaryMaxLength(200);
        platform.setTagMaxCount(5);
        platform.setContentMaxLength(100000);
        platform.setRequestTimeoutSeconds(15);
        platform.setMaxRetry(3);
        return platform;
    }

    private SysArticle article() {
        SysArticle article = new SysArticle();
        article.setArticleId(100L);
        article.setTitle("测试文章");
        article.setSummary("摘要");
        article.setContent(HTML);
        article.setCoverImage("https://cdn.example.com/cover.png");
        return article;
    }

    private void mockCommonSuccessPath() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L));
        when(taskService.markRunning(1L)).thenReturn(true);
        when(platformService.getByCode("juejin")).thenReturn(platform(1));
        when(adapterRegistry.find("juejin")).thenReturn(adapter);
        when(articleService.selectSysArticleById(100L)).thenReturn(article());
        when(platformService.decryptCredentials(any())).thenReturn(Map.of("cookie", "session=abc"));
        when(articleTagMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(adapter.contentFormat()).thenReturn(DispatchContentFormat.MARKDOWN);
        when(imageMigrationService.migrate(anyString(), any(), anyMap(), anyMap(), any(), any(), anyInt()))
                .thenAnswer(invocation -> {
                    ImageMigrationResult result = new ImageMigrationResult();
                    result.setContent(invocation.getArgument(0));
                    return result;
                });
    }

    @Test
    void execute_shouldMarkSuccessAndRecordStat_whenPublishSucceeded() {
        mockCommonSuccessPath();
        when(adapter.publish(any())).thenReturn(PublishResult.ok("7123456789", "https://juejin.cn/post/7123456789"));

        executor.execute(1L);

        ArgumentCaptor<PublishContext> captor = ArgumentCaptor.forClass(PublishContext.class);
        verify(adapter).publish(captor.capture());
        PublishContext context = captor.getValue();
        assertEquals("测试文章", context.getTitle());
        assertTrue(context.getContent().contains("# 标题"), "正文应按平台格式转为 Markdown");
        assertEquals("https://blog.example.com/article/100", context.getSourceUrl());
        assertEquals("session=abc", context.getCredential().get("cookie"));
        assertEquals("6809637773935378440", context.getConfig().get("category_id"));

        verify(taskService).markSuccess(eq(1L), eq("7123456789"), eq("https://juejin.cn/post/7123456789"), anyLong());
        verify(statService).recordPublishSuccess(1L, 100L, "juejin",
                "7123456789", "https://juejin.cn/post/7123456789");
        verify(taskService, never()).markFailed(anyLong(), any(), anyString(), anyLong());
    }

    @Test
    void execute_shouldMarkFailed_whenAdapterReturnsFailure() {
        mockCommonSuccessPath();
        when(adapter.publish(any())).thenReturn(
                PublishResult.fail(DispatchErrorTypeEnum.RATE_LIMITED, "触发平台限流"));

        executor.execute(1L);

        verify(taskService).markFailed(eq(1L), eq(DispatchErrorTypeEnum.RATE_LIMITED), eq("触发平台限流"), anyLong());
        verify(statService, never()).recordPublishSuccess(anyLong(), anyLong(), anyString(), any(), any());
    }

    @Test
    void execute_shouldMarkFailed_whenContentAdaptThrows() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L));
        when(taskService.markRunning(1L)).thenReturn(true);
        when(platformService.getByCode("juejin")).thenReturn(platform(1));
        when(adapterRegistry.find("juejin")).thenReturn(adapter);
        SysArticle emptyContent = article();
        emptyContent.setContent("");
        when(articleService.selectSysArticleById(100L)).thenReturn(emptyContent);
        when(platformService.decryptCredentials(any())).thenReturn(Map.of("cookie", "session=abc"));
        when(articleTagMapper.selectList(any())).thenReturn(Collections.emptyList());

        executor.execute(1L);

        verify(taskService).markFailed(eq(1L), eq(DispatchErrorTypeEnum.CONTENT_VIOLATION), anyString(), anyLong());
        verify(adapter, never()).publish(any());
    }

    @Test
    void execute_shouldMarkFailed_whenPlatformDisabled() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L));
        when(taskService.markRunning(1L)).thenReturn(true);
        when(platformService.getByCode("juejin")).thenReturn(platform(0));

        executor.execute(1L);

        verify(taskService).markFailed(eq(1L), eq(DispatchErrorTypeEnum.PLATFORM_DISABLED), anyString(), anyLong());
        verify(adapterRegistry, never()).find(anyString());
    }

    @Test
    void execute_shouldMarkFailed_whenPlatformConfigMissing() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L));
        when(taskService.markRunning(1L)).thenReturn(true);
        when(platformService.getByCode("juejin")).thenReturn(null);

        executor.execute(1L);

        verify(taskService).markFailed(eq(1L), eq(DispatchErrorTypeEnum.PLATFORM_DISABLED), anyString(), anyLong());
    }

    @Test
    void execute_shouldMarkFailed_whenAdapterNotFound() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L));
        when(taskService.markRunning(1L)).thenReturn(true);
        when(platformService.getByCode("juejin")).thenReturn(platform(1));
        when(adapterRegistry.find("juejin")).thenReturn(null);

        executor.execute(1L);

        verify(taskService).markFailed(eq(1L), eq(DispatchErrorTypeEnum.ADAPTER_NOT_FOUND), anyString(), anyLong());
    }

    @Test
    void execute_shouldMarkFailedWithUnknownType_whenUnexpectedExceptionThrown() {
        mockCommonSuccessPath();
        when(adapter.publish(any())).thenThrow(new IllegalStateException("连接被重置"));

        executor.execute(1L);

        verify(taskService).markFailed(eq(1L), eq(DispatchErrorTypeEnum.UNKNOWN), eq("连接被重置"), anyLong());
    }

    @Test
    void execute_shouldSkip_whenTaskAlreadySucceeded() {
        BlogDispatchTask finished = task(1L);
        finished.setStatus(DispatchStatusEnum.SUCCESS.getCode());
        when(taskMapper.selectById(1L)).thenReturn(finished);

        executor.execute(1L);

        verify(taskService, never()).markRunning(anyLong());
        verify(taskService, never()).markSuccess(anyLong(), any(), any(), anyLong());
    }

    @Test
    void execute_shouldSkip_whenTaskGrabbedByAnotherThread() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L));
        when(taskService.markRunning(1L)).thenReturn(false);

        executor.execute(1L);

        verify(platformService, never()).getByCode(anyString());
        verify(taskService, never()).markFailed(anyLong(), any(), anyString(), anyLong());
    }

    @Test
    void execute_shouldIgnoreUnknownTask() {
        when(taskMapper.selectById(1L)).thenReturn(null);

        executor.execute(1L);

        verify(taskService, never()).markRunning(anyLong());
        verifyNoInteractions(adapterRegistry);
    }
}
