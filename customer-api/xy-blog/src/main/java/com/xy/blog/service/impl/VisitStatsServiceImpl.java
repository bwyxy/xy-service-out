package com.xy.blog.service.impl;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.blog.pojo.dto.BlogVisitTrackDTO;
import com.xy.blog.pojo.dto.VisitStatsQuery;
import com.xy.blog.service.IVisitStatsService;
import com.xy.blog.service.VisitStatsCollector;
import com.xy.common.entity.blog.BlogEventLog;
import com.xy.common.ip.IpUtils;
import com.xy.common.utils.StringUtils;
import com.xy.common.vo.blog.VisitDailyStatsVO;
import com.xy.common.vo.blog.VisitDeviceVO;
import com.xy.common.vo.blog.VisitOverviewVO;
import com.xy.common.vo.blog.VisitTopPageVO;
import com.xy.common.vo.blog.VisitTopSourceVO;
import com.xy.system.mapper.blog.BlogEventLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网站访问统计服务实现
 */
@Slf4j
@Service
public class VisitStatsServiceImpl extends ServiceImpl<BlogEventLogMapper, BlogEventLog>
        implements IVisitStatsService {

    private final VisitStatsCollector collector;
    private final StringRedisTemplate stringRedisTemplate;
    private final BlogEventLogMapper blogEventLogMapper;

    public VisitStatsServiceImpl(VisitStatsCollector collector,
                                 StringRedisTemplate stringRedisTemplate,
                                 BlogEventLogMapper blogEventLogMapper) {
        this.collector = collector;
        this.stringRedisTemplate = stringRedisTemplate;
        this.blogEventLogMapper = blogEventLogMapper;
    }

    @Override
    public void track(BlogVisitTrackDTO dto, HttpServletRequest request, Long loginUserId) {
        String ip = IpUtils.getIpAddr(request);
        String ua = request.getHeader("User-Agent");
        String visitorId = StringUtils.isBlank(dto.getVisitorId()) ? ip : dto.getVisitorId();
        String sessionId = StringUtils.isBlank(dto.getSessionId()) ? visitorId : dto.getSessionId();

        BlogEventLog event = new BlogEventLog();
        event.setEventType(dto.getEventType());
        event.setPath(trimToLength(dto.getPath(), 512));
        event.setReferrer(trimToLength(dto.getReferrer(), 512));
        event.setTitle(trimToLength(dto.getTitle(), 240));
        event.setVisitorId(trimToLength(visitorId, 64));
        event.setSessionId(trimToLength(sessionId, 64));
        event.setLoginUserId(loginUserId);
        event.setIp(trimToLength(ip, 64));
        event.setUa(trimToLength(ua, 1024));

        UserAgent userAgent = UserAgentUtil.parse(ua);
        event.setDevice(userAgent.isMobile() ? "Mobile" : "PC");
        event.setOs(userAgent.getOs().getName());
        event.setBrowser(userAgent.getBrowser().getName());

        Map<String, Object> props = new HashMap<>();
        if (dto.getDuration() != null) {
            props.put("duration", dto.getDuration());
        }
        if (StringUtils.isNotBlank(dto.getSource())) {
            props.put("source", dto.getSource());
        }
        if (dto.getProps() != null) {
            props.putAll(dto.getProps());
        }
        if (!props.isEmpty()) {
            event.setProps(JSON.toJSONString(props));
        }

        event.setCreatedAt(LocalDateTime.now());
        collector.collect(event);
    }

    @Override
    public VisitOverviewVO getOverview() {
        VisitOverviewVO vo = new VisitOverviewVO();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        VisitDailyStatsVO todayStats = getRedisDailyStats(today);
        VisitDailyStatsVO yesterdayStats = blogEventLogMapper.selectDayStats(
                yesterday.atStartOfDay(), yesterday.atTime(LocalTime.MAX));
        VisitDailyStatsVO totalStats = blogEventLogMapper.selectTotalPvUv(
                today.atStartOfDay().minusNanos(1));

        if (totalStats == null) {
            totalStats = new VisitDailyStatsVO();
        }
        if (yesterdayStats == null) {
            yesterdayStats = new VisitDailyStatsVO();
        }

        vo.setTotalPv(nvl(totalStats.getPv()) + nvl(todayStats.getPv()));
        vo.setTotalUv(nvl(totalStats.getUv()) + nvl(todayStats.getUv()));
        vo.setTodayPv(nvl(todayStats.getPv()));
        vo.setTodayUv(nvl(todayStats.getUv()));
        vo.setYesterdayPv(nvl(yesterdayStats.getPv()));
        vo.setYesterdayUv(nvl(yesterdayStats.getUv()));
        vo.setTodayAvgDurationSec(todayStats.getAvgDurationSec());
        return vo;
    }

    @Override
    public List<VisitDailyStatsVO> getDailyStats(VisitStatsQuery query) {
        LocalDate start = query.getStartDate() != null ? query.getStartDate() : LocalDate.now().minusDays(6);
        LocalDate end = query.getEndDate() != null ? query.getEndDate() : LocalDate.now();
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        LocalDate today = LocalDate.now();
        Map<LocalDate, VisitDailyStatsVO> dbMap = Collections.emptyMap();
        if (start.isBefore(today)) {
            LocalDate dbEnd = end.isBefore(today) ? end : today.minusDays(1);
            List<VisitDailyStatsVO> dbList = blogEventLogMapper.selectDailyStats(
                    start.atStartOfDay(), dbEnd.atTime(LocalTime.MAX));
            dbMap = dbList.stream().collect(Collectors.toMap(VisitDailyStatsVO::getStatDate, v -> v));
        }

        VisitDailyStatsVO todayStats = null;
        if (!end.isBefore(today)) {
            todayStats = getRedisDailyStats(today);
        }

        List<VisitDailyStatsVO> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            VisitDailyStatsVO item;
            if (date.equals(today) && todayStats != null) {
                item = todayStats;
                item.setStatDate(date);
            } else {
                item = dbMap.getOrDefault(date, emptyDailyStats(date));
            }
            fillAvgDuration(item);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<VisitTopPageVO> getTopPages(VisitStatsQuery query) {
        LocalDate[] range = normalizeRange(query);
        List<VisitTopPageVO> dbList = blogEventLogMapper.selectTopPages(
                range[0].atStartOfDay(), range[1].atTime(LocalTime.MAX), query.getTopN());
        if (range[1].equals(LocalDate.now())) {
            return mergeTodayTopPages(dbList, query.getTopN());
        }
        return dbList;
    }

    @Override
    public List<VisitTopSourceVO> getTopSources(VisitStatsQuery query) {
        LocalDate[] range = normalizeRange(query);
        List<VisitTopSourceVO> dbList = blogEventLogMapper.selectTopSources(
                range[0].atStartOfDay(), range[1].atTime(LocalTime.MAX), query.getTopN());
        if (range[1].equals(LocalDate.now())) {
            return mergeTodayTopSources(dbList, query.getTopN());
        }
        return dbList;
    }

    @Override
    public List<VisitDeviceVO> getDeviceDistribution(VisitStatsQuery query) {
        LocalDate[] range = normalizeRange(query);
        List<VisitDeviceVO> dbList = blogEventLogMapper.selectDeviceDistribution(
                range[0].atStartOfDay(), range[1].atTime(LocalTime.MAX));
        if (range[1].equals(LocalDate.now())) {
            return mergeTodayDeviceStats(dbList);
        }
        return dbList;
    }

    private VisitDailyStatsVO getRedisDailyStats(LocalDate date) {
        VisitDailyStatsVO vo = new VisitDailyStatsVO();
        vo.setStatDate(date);
        String dailyKey = collector.dailyKey(date);
        String uvKey = collector.uvKey(date);
        String sessionKey = collector.sessionKey(date);

        Object pvObj = stringRedisTemplate.opsForHash().get(dailyKey, "pv");
        vo.setPv(parseLong(pvObj));
        vo.setUv(stringRedisTemplate.opsForHyperLogLog().size(uvKey));
        vo.setSessionCount(stringRedisTemplate.opsForHyperLogLog().size(sessionKey));

        Object durCountObj = stringRedisTemplate.opsForHash().get(dailyKey, "durationCount");
        Object durTotalObj = stringRedisTemplate.opsForHash().get(dailyKey, "durationTotal");
        vo.setDurationCount(parseLong(durCountObj));
        vo.setTotalDurationMs(parseLong(durTotalObj));
        fillAvgDuration(vo);
        return vo;
    }

    private List<VisitTopPageVO> mergeTodayTopPages(List<VisitTopPageVO> dbList, int topN) {
        Map<String, VisitTopPageVO> map = dbList.stream()
                .collect(Collectors.toMap(VisitTopPageVO::getPath, v -> v, (a, b) -> a));
        String key = collector.pageKey(LocalDate.now());
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String path = entry.getKey().toString();
            long pv = parseLong(entry.getValue());
            map.computeIfAbsent(path, k -> {
                VisitTopPageVO vo = new VisitTopPageVO();
                vo.setPath(path);
                return vo;
            }).setPv(nvl(map.get(path).getPv()) + pv);
        }
        List<VisitTopPageVO> merged = new ArrayList<>(map.values());
        merged.sort((a, b) -> Long.compare(nvl(b.getPv()), nvl(a.getPv())));
        return merged.stream().limit(topN).toList();
    }

    private List<VisitTopSourceVO> mergeTodayTopSources(List<VisitTopSourceVO> dbList, int topN) {
        Map<String, VisitTopSourceVO> map = dbList.stream()
                .collect(Collectors.toMap(VisitTopSourceVO::getReferrer, v -> v, (a, b) -> a));
        String key = collector.sourceKey(LocalDate.now());
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String referrer = entry.getKey().toString();
            long pv = parseLong(entry.getValue());
            map.computeIfAbsent(referrer, k -> {
                VisitTopSourceVO vo = new VisitTopSourceVO();
                vo.setReferrer(referrer);
                return vo;
            }).setPv(nvl(map.get(referrer).getPv()) + pv);
        }
        List<VisitTopSourceVO> merged = new ArrayList<>(map.values());
        merged.sort((a, b) -> Long.compare(nvl(b.getPv()), nvl(a.getPv())));
        return merged.stream().limit(topN).toList();
    }

    private List<VisitDeviceVO> mergeTodayDeviceStats(List<VisitDeviceVO> dbList) {
        Map<String, VisitDeviceVO> map = dbList.stream()
                .collect(Collectors.toMap(VisitDeviceVO::getDevice, v -> v, (a, b) -> a));
        String key = collector.deviceKey(LocalDate.now());
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String device = entry.getKey().toString();
            long count = parseLong(entry.getValue());
            map.computeIfAbsent(device, k -> {
                VisitDeviceVO vo = new VisitDeviceVO();
                vo.setDevice(device);
                return vo;
            }).setCount(nvl(map.get(device).getCount()) + count);
        }
        List<VisitDeviceVO> merged = new ArrayList<>(map.values());
        merged.sort((a, b) -> Long.compare(nvl(b.getCount()), nvl(a.getCount())));
        return merged;
    }

    private LocalDate[] normalizeRange(VisitStatsQuery query) {
        LocalDate start = query.getStartDate() != null ? query.getStartDate() : LocalDate.now().minusDays(6);
        LocalDate end = query.getEndDate() != null ? query.getEndDate() : LocalDate.now();
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        return new LocalDate[]{start, end};
    }

    private VisitDailyStatsVO emptyDailyStats(LocalDate date) {
        VisitDailyStatsVO vo = new VisitDailyStatsVO();
        vo.setStatDate(date);
        vo.setPv(0L);
        vo.setUv(0L);
        vo.setSessionCount(0L);
        vo.setDurationCount(0L);
        vo.setTotalDurationMs(0L);
        vo.setAvgDurationSec(0.0);
        return vo;
    }

    private void fillAvgDuration(VisitDailyStatsVO vo) {
        long count = nvl(vo.getDurationCount());
        long total = nvl(vo.getTotalDurationMs());
        if (count > 0 && total > 0) {
            vo.setAvgDurationSec(Math.round(total * 1.0 / count / 1000.0 * 100.0) / 100.0);
        } else {
            vo.setAvgDurationSec(0.0);
        }
    }

    private long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
