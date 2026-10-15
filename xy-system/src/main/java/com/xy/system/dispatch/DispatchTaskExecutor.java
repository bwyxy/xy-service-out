package com.xy.system.dispatch;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.common.entity.SysArticle;
import com.xy.common.entity.SysArticleTag;
import com.xy.common.entity.SysTag;
import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.entity.blog.BlogDispatchTask;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.common.enums.DispatchStatusEnum;
import com.xy.system.dispatch.adapter.DispatchAdapterRegistry;
import com.xy.system.dispatch.adapter.DispatchPlatformAdapter;
import com.xy.system.dispatch.adapter.model.PublishContext;
import com.xy.system.dispatch.adapter.model.PublishResult;
import com.xy.system.dispatch.content.AdaptedContent;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分发任务异步执行器
 * 单条任务的全流程：抢占任务 -> 组装上下文（格式适配 + 图片迁移）-> 调用平台适配器发布
 * -> 回写任务状态与平台访问链接 -> 登记运营数据记录
 * */
@Slf4j
@Component
public class DispatchTaskExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 15;

    private final BlogDispatchTaskMapper taskMapper;

    private final IBlogDispatchTaskService taskService;

    private final IBlogDispatchPlatformService platformService;

    private final IBlogDispatchStatService statService;

    private final ISysArticleService articleService;

    private final SysArticleTagMapper articleTagMapper;

    private final SysTagMapper tagMapper;

    private final DispatchAdapterRegistry adapterRegistry;

    private final ContentAdaptService contentAdaptService;

    private final ImageMigrationService imageMigrationService;

    /** 自己博客的站点前缀，用于回传原文地址 */
    @Value("${dispatch.article-url-prefix:}")
    private String articleUrlPrefix;

    public DispatchTaskExecutor(BlogDispatchTaskMapper taskMapper,
                                IBlogDispatchTaskService taskService,
                                IBlogDispatchPlatformService platformService,
                                IBlogDispatchStatService statService,
                                ISysArticleService articleService,
                                SysArticleTagMapper articleTagMapper,
                                SysTagMapper tagMapper,
                                DispatchAdapterRegistry adapterRegistry,
                                ContentAdaptService contentAdaptService,
                                ImageMigrationService imageMigrationService) {
        this.taskMapper = taskMapper;
        this.taskService = taskService;
        this.platformService = platformService;
        this.statService = statService;
        this.articleService = articleService;
        this.articleTagMapper = articleTagMapper;
        this.tagMapper = tagMapper;
        this.adapterRegistry = adapterRegistry;
        this.contentAdaptService = contentAdaptService;
        this.imageMigrationService = imageMigrationService;
    }

    /**
     * 异步执行单条分发任务
     * */
    @Async("dispatchExecutor")
    public void execute(Long taskId) {
        if (taskId == null) {
            return;
        }
        BlogDispatchTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("分发任务不存在，跳过 taskId={}", taskId);
            return;
        }
        if (DispatchStatusEnum.SUCCESS.getCode().equals(task.getStatus())) {
            log.debug("分发任务已成功，跳过重复执行 taskId={}", taskId);
            return;
        }
        if (!taskService.markRunning(taskId)) {
            log.debug("分发任务已被其他线程抢占，跳过 taskId={}", taskId);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            doExecute(task, start);
        } catch (DispatchException e) {
            taskService.markFailed(taskId, e.getErrorType(), e.getMessage(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("分发任务执行出现未预期异常 taskId={}", taskId, e);
            taskService.markFailed(taskId, DispatchErrorTypeEnum.UNKNOWN,
                    rootMessage(e), System.currentTimeMillis() - start);
        }
    }

    private void doExecute(BlogDispatchTask task, long start) {
        BlogDispatchPlatform platform = platformService.getByCode(task.getPlatformCode());
        if (platform == null) {
            throw new DispatchException(DispatchErrorTypeEnum.PLATFORM_DISABLED,
                    "平台配置不存在，请先在分发平台配置中创建：" + task.getPlatformCode());
        }
        if (platform.getEnabled() == null || platform.getEnabled() != 1) {
            throw new DispatchException(DispatchErrorTypeEnum.PLATFORM_DISABLED,
                    "平台【" + platform.getPlatformName() + "】未启用，请先在平台配置中开启");
        }
        DispatchPlatformAdapter adapter = adapterRegistry.find(task.getPlatformCode());
        if (adapter == null) {
            throw new DispatchException(DispatchErrorTypeEnum.ADAPTER_NOT_FOUND,
                    "未找到平台【" + task.getPlatformCode() + "】的适配器实现");
        }
        SysArticle article = articleService.selectSysArticleById(task.getArticleId());
        if (article == null) {
            throw new DispatchException(DispatchErrorTypeEnum.PLATFORM_ERROR, "文章不存在或已删除，无法分发");
        }

        Map<String, String> credential = platformService.decryptCredentials(platform);
        adapter.validateCredential(credential);
        Map<String, Object> config = parseConfig(platform.getConfigJson());
        int timeout = platform.getRequestTimeoutSeconds() == null
                ? DEFAULT_TIMEOUT_SECONDS : platform.getRequestTimeoutSeconds();

        List<String> tagNames = loadTagNames(article.getArticleId());
        AdaptedContent adapted = contentAdaptService.adapt(article.getTitle(), article.getSummary(),
                article.getContent(), tagNames, platform, adapter);
        if (!adapted.getWarnings().isEmpty()) {
            log.info("任务 {} 内容适配提示：{}", task.getId(), adapted.getWarnings());
        }

        ImageMigrationResult migration = imageMigrationService.migrate(adapted.getContent(),
                adapter.contentFormat(), credential, config, platform, adapter, timeout);
        if (migration.getFailedCount() > 0) {
            log.warn("任务 {} 有 {} 张图片迁移失败：{}", task.getId(), migration.getFailedCount(),
                    migration.getFailures());
        }

        PublishContext context = new PublishContext();
        context.setArticleId(article.getArticleId());
        context.setTitle(adapted.getTitle());
        context.setSummary(adapted.getSummary());
        context.setContent(migration.getContent());
        context.setCoverImage(article.getCoverImage());
        context.setTags(adapted.getTags());
        context.setSourceUrl(buildSourceUrl(article));
        context.setCredential(credential);
        context.setConfig(config);
        context.setPlatform(platform);
        context.setTimeoutSeconds(timeout);
        context.setMaxRetry(platform.getMaxRetry() == null ? 3 : platform.getMaxRetry());

        PublishResult result = adapter.publish(context);
        long cost = System.currentTimeMillis() - start;
        if (result.isSuccess()) {
            taskService.markSuccess(task.getId(), result.getPlatformArticleId(), result.getResultUrl(), cost);
            statService.recordPublishSuccess(task.getId(), article.getArticleId(), task.getPlatformCode(),
                    result.getPlatformArticleId(), result.getResultUrl());
            log.info("分发成功 taskId={} platform={} articleId={} cost={}ms",
                    task.getId(), task.getPlatformCode(), article.getArticleId(), cost);
            return;
        }
        taskService.markFailed(task.getId(), result.getErrorType(), result.getErrorMsg(), cost);
        log.warn("分发失败 taskId={} platform={} type={} msg={}", task.getId(), task.getPlatformCode(),
                result.getErrorType().getCode(), result.getErrorMsg());
    }

    /**
     * 查询文章标签名称，用于按平台限制裁剪标签
     * */
    private List<String> loadTagNames(Long articleId) {
        if (articleId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArticleTag::getArticleId, articleId);
        List<SysArticleTag> relations = articleTagMapper.selectList(wrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = new ArrayList<>(relations.size());
        for (SysArticleTag relation : relations) {
            if (relation.getTagId() != null) {
                tagIds.add(relation.getTagId());
            }
        }
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (SysTag tag : tagMapper.selectBatchIds(tagIds)) {
            if (tag.getName() != null && !tag.getName().isBlank()) {
                names.add(tag.getName());
            }
        }
        return names;
    }

    private String buildSourceUrl(SysArticle article) {
        if (articleUrlPrefix == null || articleUrlPrefix.isBlank() || article.getArticleId() == null) {
            return null;
        }
        String prefix = articleUrlPrefix.endsWith("/")
                ? articleUrlPrefix.substring(0, articleUrlPrefix.length() - 1) : articleUrlPrefix;
        return prefix + "/article/" + article.getArticleId();
    }

    private Map<String, Object> parseConfig(String configJson) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (configJson == null || configJson.isBlank()) {
            return config;
        }
        try {
            JSONObject json = JSONUtil.parseObj(configJson);
            for (String key : json.keySet()) {
                config.put(key, json.get(key));
            }
        } catch (Exception e) {
            log.warn("平台发布规则解析失败 err={}", e.getMessage());
        }
        return config;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
