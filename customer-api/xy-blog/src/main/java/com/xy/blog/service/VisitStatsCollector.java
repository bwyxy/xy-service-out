package com.xy.blog.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.blog.BlogEventLog;
import com.xy.system.mapper.blog.BlogEventLogMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 访问统计事件收集器
 */
@Slf4j
@Service
public class VisitStatsCollector extends ServiceImpl<BlogEventLogMapper, BlogEventLog> {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String QUEUE_KEY = "blog:visit:queue";
    private static final String DAILY_KEY_PREFIX = "blog:visit:daily:";
    private static final String UV_KEY_PREFIX = "blog:visit:uv:";
    private static final String SESSION_KEY_PREFIX = "blog:visit:session:";
    private static final String PAGE_KEY_PREFIX = "blog:visit:page:";
    private static final String SOURCE_KEY_PREFIX = "blog:visit:source:";
    private static final String DEVICE_KEY_PREFIX = "blog:visit:device:";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int FLUSH_BATCH_SIZE = 500;

    public VisitStatsCollector(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        // 启动时触发一次刷新, 避免上次宕机残留数据丢失
        flushQueue();
    }

    /**
     * 异步接收访问事件并入队
     */
    @Async("visitStatsExecutor")
    public void collect(BlogEventLog event) {
        try {
            stringRedisTemplate.opsForList().rightPush(QUEUE_KEY, JSON.toJSONString(event));
            updateAggregates(event);
        } catch (Exception e) {
            log.warn("访问事件收集异常", e);
        }
    }

    /**
     * 定时将队列中的事件批量写入数据库
     */
    @Scheduled(fixedDelay = 30_000)
    public void flushQueue() {
        List<String> batch = new ArrayList<>();
        String json;
        while ((json = stringRedisTemplate.opsForList().leftPop(QUEUE_KEY)) != null
                && batch.size() < FLUSH_BATCH_SIZE) {
            batch.add(json);
        }
        if (batch.isEmpty()) {
            return;
        }
        List<BlogEventLog> events = new ArrayList<>(batch.size());
        for (String item : batch) {
            try {
                events.add(JSON.parseObject(item, BlogEventLog.class));
            } catch (Exception e) {
                log.warn("访问事件反序列化失败: {}", item, e);
            }
        }
        if (events.isEmpty()) {
            return;
        }
        try {
            saveBatch(events, 200);
            log.debug("批量刷新访问事件 {} 条", events.size());
        } catch (Exception e) {
            log.error("访问事件批量入库失败", e);
        }
    }

    private void updateAggregates(BlogEventLog event) {
        String date = LocalDate.now().format(DATE_FORMAT);
        String dailyKey = DAILY_KEY_PREFIX + date;
        String visitorKey = event.getVisitorId() == null ? event.getIp() : event.getVisitorId();
        String sessionKey = event.getSessionId() == null ? visitorKey : event.getSessionId();

        if ("page_view".equals(event.getEventType())) {
            stringRedisTemplate.opsForHash().increment(dailyKey, "pv", 1);
            stringRedisTemplate.opsForHyperLogLog().add(UV_KEY_PREFIX + date, visitorKey);
            stringRedisTemplate.opsForHyperLogLog().add(SESSION_KEY_PREFIX + date, sessionKey);

            String path = event.getPath();
            if (path != null && !path.isEmpty()) {
                stringRedisTemplate.opsForHash().increment(PAGE_KEY_PREFIX + date, path, 1);
            }
            String referrer = event.getReferrer();
            if (referrer != null && !referrer.isEmpty()) {
                stringRedisTemplate.opsForHash().increment(SOURCE_KEY_PREFIX + date, referrer, 1);
            }
            String device = event.getDevice();
            stringRedisTemplate.opsForHash().increment(DEVICE_KEY_PREFIX + date,
                    device == null || device.isEmpty() ? "未知" : device, 1);
        }

        if ("duration".equals(event.getEventType()) && event.getProps() != null) {
            Integer duration = parseDuration(event.getProps());
            if (duration != null && duration > 0) {
                stringRedisTemplate.opsForHash().increment(dailyKey, "durationCount", 1);
                stringRedisTemplate.opsForHash().increment(dailyKey, "durationTotal", duration);
            }
        }
    }

    private Integer parseDuration(String props) {
        try {
            return JSON.parseObject(props).getInteger("duration");
        } catch (Exception e) {
            return null;
        }
    }

    public String dailyKey(LocalDate date) {
        return DAILY_KEY_PREFIX + date.format(DATE_FORMAT);
    }

    public String uvKey(LocalDate date) {
        return UV_KEY_PREFIX + date.format(DATE_FORMAT);
    }

    public String sessionKey(LocalDate date) {
        return SESSION_KEY_PREFIX + date.format(DATE_FORMAT);
    }

    public String pageKey(LocalDate date) {
        return PAGE_KEY_PREFIX + date.format(DATE_FORMAT);
    }

    public String sourceKey(LocalDate date) {
        return SOURCE_KEY_PREFIX + date.format(DATE_FORMAT);
    }

    public String deviceKey(LocalDate date) {
        return DEVICE_KEY_PREFIX + date.format(DATE_FORMAT);
    }
}
