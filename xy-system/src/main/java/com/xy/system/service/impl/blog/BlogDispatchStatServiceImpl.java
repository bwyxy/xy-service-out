package com.xy.system.service.impl.blog;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.dto.blog.DispatchStatQueryDTO;
import com.xy.common.entity.blog.BlogDispatchArticleStat;
import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.entity.blog.BlogDispatchTask;
import com.xy.common.enums.DispatchStatusEnum;
import com.xy.common.vo.blog.DispatchStatSummaryVO;
import com.xy.system.dispatch.adapter.DispatchAdapterRegistry;
import com.xy.system.dispatch.adapter.DispatchPlatformAdapter;
import com.xy.system.dispatch.adapter.model.PublishedRef;
import com.xy.system.dispatch.adapter.model.StatFetchRequest;
import com.xy.system.dispatch.adapter.model.StatResult;
import com.xy.system.mapper.blog.BlogDispatchArticleStatMapper;
import com.xy.system.mapper.blog.BlogDispatchTaskMapper;
import com.xy.system.service.blog.IBlogDispatchPlatformService;
import com.xy.system.service.blog.IBlogDispatchStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 分发运营数据服务实现
 * 分发成功时登记记录，定时任务按平台批量采集浏览量/点赞/评论/收藏并回写
 * */
@Slf4j
@Service
public class BlogDispatchStatServiceImpl extends ServiceImpl<BlogDispatchArticleStatMapper, BlogDispatchArticleStat>
        implements IBlogDispatchStatService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 15;

    private final IBlogDispatchPlatformService platformService;

    private final DispatchAdapterRegistry adapterRegistry;

    private final BlogDispatchTaskMapper taskMapper;

    public BlogDispatchStatServiceImpl(IBlogDispatchPlatformService platformService,
                                       DispatchAdapterRegistry adapterRegistry,
                                       BlogDispatchTaskMapper taskMapper) {
        this.platformService = platformService;
        this.adapterRegistry = adapterRegistry;
        this.taskMapper = taskMapper;
    }

    @Override
    public PageResult<BlogDispatchArticleStat> selectStatPage(DispatchStatQueryDTO query) {
        DispatchStatQueryDTO condition = query == null ? new DispatchStatQueryDTO() : query;
        Page<BlogDispatchArticleStat> page = new Page<>(condition.getPageNum(), condition.getPageSize());
        IPage<BlogDispatchArticleStat> result = baseMapper.selectStatPage(page, condition);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<DispatchStatSummaryVO> selectPlatformSummary(String platformCode) {
        return baseMapper.selectPlatformSummary(platformCode);
    }

    @Override
    public void recordPublishSuccess(Long taskId, Long articleId, String platformCode,
                                     String platformArticleId, String resultUrl) {
        if (taskId == null) {
            return;
        }
        BlogDispatchArticleStat existing = getByTaskId(taskId);
        if (existing == null) {
            BlogDispatchArticleStat stat = new BlogDispatchArticleStat();
            stat.setTaskId(taskId);
            stat.setArticleId(articleId);
            stat.setPlatformCode(platformCode);
            stat.setPlatformArticleId(platformArticleId);
            stat.setResultUrl(resultUrl);
            stat.setViewCount(0);
            stat.setLikeCount(0);
            stat.setCommentCount(0);
            stat.setCollectCount(0);
            stat.setStatTime(LocalDateTime.now());
            save(stat);
            return;
        }
        LambdaUpdateWrapper<BlogDispatchArticleStat> update = new LambdaUpdateWrapper<>();
        update.eq(BlogDispatchArticleStat::getId, existing.getId())
                .set(BlogDispatchArticleStat::getPlatformArticleId, platformArticleId)
                .set(BlogDispatchArticleStat::getResultUrl, resultUrl)
                .set(BlogDispatchArticleStat::getStatTime, LocalDateTime.now());
        update(update);
    }

    @Override
    public int refreshStats() {
        LambdaQueryWrapper<BlogDispatchTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDispatchTask::getStatus, DispatchStatusEnum.SUCCESS.getCode())
                .isNotNull(BlogDispatchTask::getPlatformArticleId)
                .ne(BlogDispatchTask::getPlatformArticleId, "");
        List<BlogDispatchTask> tasks = taskMapper.selectList(wrapper);
        if (tasks.isEmpty()) {
            return 0;
        }

        Map<String, List<BlogDispatchTask>> grouped = new LinkedHashMap<>();
        for (BlogDispatchTask task : tasks) {
            grouped.computeIfAbsent(task.getPlatformCode(), key -> new ArrayList<>()).add(task);
        }

        int updated = 0;
        for (Map.Entry<String, List<BlogDispatchTask>> entry : grouped.entrySet()) {
            updated += refreshPlatform(entry.getKey(), entry.getValue());
        }
        return updated;
    }

    private int refreshPlatform(String platformCode, List<BlogDispatchTask> tasks) {
        BlogDispatchPlatform platform = platformService.getByCode(platformCode);
        if (platform == null || platform.getEnabled() == null || platform.getEnabled() != 1) {
            return 0;
        }
        DispatchPlatformAdapter adapter = adapterRegistry.find(platformCode);
        if (adapter == null || !adapter.supportsStatFetch()) {
            return 0;
        }
        Map<String, String> credential = platformService.decryptCredentials(platform);
        if (credential.isEmpty()) {
            log.debug("平台【{}】未配置凭证，跳过运营数据采集", platform.getPlatformName());
            return 0;
        }

        Map<String, List<BlogDispatchTask>> byArticleId = new LinkedHashMap<>();
        for (BlogDispatchTask task : tasks) {
            byArticleId.computeIfAbsent(task.getPlatformArticleId(), key -> new ArrayList<>()).add(task);
        }

        List<PublishedRef> refs = new ArrayList<>();
        for (String platformArticleId : new LinkedHashSet<>(byArticleId.keySet())) {
            BlogDispatchTask sample = byArticleId.get(platformArticleId).get(0);
            refs.add(new PublishedRef(platformArticleId, sample.getResultUrl()));
        }

        StatFetchRequest request = new StatFetchRequest();
        request.setRefs(refs);
        request.setCredential(credential);
        request.setConfig(parseConfig(platform.getConfigJson()));
        request.setPlatform(platform);
        request.setTimeoutSeconds(platform.getRequestTimeoutSeconds() == null
                ? DEFAULT_TIMEOUT_SECONDS : platform.getRequestTimeoutSeconds());

        List<StatResult> results;
        try {
            results = adapter.fetchStats(request);
        } catch (Exception e) {
            log.warn("平台【{}】运营数据采集失败 err={}", platform.getPlatformName(), e.getMessage());
            return 0;
        }

        int updated = 0;
        for (StatResult result : results) {
            List<BlogDispatchTask> matched = byArticleId.get(result.getPlatformArticleId());
            if (matched == null) {
                continue;
            }
            for (BlogDispatchTask task : matched) {
                saveStat(task, result);
                updated++;
            }
        }
        return updated;
    }

    private void saveStat(BlogDispatchTask task, StatResult result) {
        BlogDispatchArticleStat existing = getByTaskId(task.getId());
        if (existing == null) {
            BlogDispatchArticleStat stat = new BlogDispatchArticleStat();
            stat.setTaskId(task.getId());
            stat.setArticleId(task.getArticleId());
            stat.setPlatformCode(task.getPlatformCode());
            stat.setPlatformArticleId(result.getPlatformArticleId());
            stat.setResultUrl(task.getResultUrl());
            stat.setViewCount(result.getViewCount());
            stat.setLikeCount(result.getLikeCount());
            stat.setCommentCount(result.getCommentCount());
            stat.setCollectCount(result.getCollectCount());
            stat.setStatTime(LocalDateTime.now());
            save(stat);
            return;
        }
        LambdaUpdateWrapper<BlogDispatchArticleStat> update = new LambdaUpdateWrapper<>();
        update.eq(BlogDispatchArticleStat::getId, existing.getId())
                .set(BlogDispatchArticleStat::getViewCount, result.getViewCount())
                .set(BlogDispatchArticleStat::getLikeCount, result.getLikeCount())
                .set(BlogDispatchArticleStat::getCommentCount, result.getCommentCount())
                .set(BlogDispatchArticleStat::getCollectCount, result.getCollectCount())
                .set(BlogDispatchArticleStat::getStatTime, LocalDateTime.now());
        update(update);
    }

    private BlogDispatchArticleStat getByTaskId(Long taskId) {
        LambdaQueryWrapper<BlogDispatchArticleStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDispatchArticleStat::getTaskId, taskId);
        return getOne(wrapper, false);
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
}
