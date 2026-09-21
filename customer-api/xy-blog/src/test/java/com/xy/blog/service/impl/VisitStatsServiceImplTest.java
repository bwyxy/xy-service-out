package com.xy.blog.service.impl;

import com.xy.blog.pojo.dto.BlogVisitTrackDTO;
import com.xy.blog.pojo.dto.VisitStatsQuery;
import com.xy.blog.service.VisitStatsCollector;
import com.xy.common.entity.blog.BlogEventLog;
import com.xy.common.vo.blog.VisitDailyStatsVO;
import com.xy.common.vo.blog.VisitDeviceVO;
import com.xy.common.vo.blog.VisitOverviewVO;
import com.xy.common.vo.blog.VisitTopPageVO;
import com.xy.system.mapper.blog.BlogEventLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * 访问统计服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class VisitStatsServiceImplTest {

    @Mock
    private VisitStatsCollector collector;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private BlogEventLogMapper blogEventLogMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private VisitStatsServiceImpl visitStatsService;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private HyperLogLogOperations<String, String> hyperLogLogOperations;

    @BeforeEach
    void setUp() {
        lenient().when(collector.dailyKey(any())).thenAnswer(inv -> "blog:visit:daily:" + inv.getArgument(0));
        lenient().when(collector.uvKey(any())).thenAnswer(inv -> "blog:visit:uv:" + inv.getArgument(0));
        lenient().when(collector.sessionKey(any())).thenAnswer(inv -> "blog:visit:session:" + inv.getArgument(0));
        lenient().when(collector.pageKey(any())).thenAnswer(inv -> "blog:visit:page:" + inv.getArgument(0));
        lenient().when(collector.sourceKey(any())).thenAnswer(inv -> "blog:visit:source:" + inv.getArgument(0));
        lenient().when(collector.deviceKey(any())).thenAnswer(inv -> "blog:visit:device:" + inv.getArgument(0));
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(stringRedisTemplate.opsForHyperLogLog()).thenReturn(hyperLogLogOperations);
    }

    @Test
    void track_shouldBuildEventAndCallCollector() {
        BlogVisitTrackDTO dto = new BlogVisitTrackDTO();
        dto.setEventType("page_view");
        dto.setPath("/article/1");
        dto.setTitle("测试文章");
        dto.setVisitorId("visitor-1");
        dto.setSessionId("session-1");

        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        when(request.getHeader("x-forwarded-for")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        visitStatsService.track(dto, request, 100L);

        ArgumentCaptor<BlogEventLog> captor = ArgumentCaptor.forClass(BlogEventLog.class);
        verify(collector, times(1)).collect(captor.capture());
        BlogEventLog event = captor.getValue();
        assertEquals("page_view", event.getEventType());
        assertEquals("/article/1", event.getPath());
        assertEquals("visitor-1", event.getVisitorId());
        assertEquals(100L, event.getLoginUserId());
        assertEquals("PC", event.getDevice());
        assertNotNull(event.getBrowser());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void getDailyStats_shouldReturnHistoricalOnly() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        VisitStatsQuery query = new VisitStatsQuery();
        query.setStartDate(yesterday);
        query.setEndDate(yesterday);

        VisitDailyStatsVO vo = new VisitDailyStatsVO();
        vo.setStatDate(yesterday);
        vo.setPv(10L);
        vo.setUv(5L);
        vo.setSessionCount(5L);
        vo.setDurationCount(2L);
        vo.setTotalDurationMs(3000L);

        when(blogEventLogMapper.selectDailyStats(yesterday.atStartOfDay(), yesterday.atTime(LocalTime.MAX)))
                .thenReturn(Collections.singletonList(vo));

        List<VisitDailyStatsVO> result = visitStatsService.getDailyStats(query);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getPv());
        assertEquals(1.5, result.get(0).getAvgDurationSec());
        verify(stringRedisTemplate, never()).opsForHash();
    }

    @Test
    void getDailyStats_shouldMergeTodayRedisStats() {
        LocalDate today = LocalDate.now();
        VisitStatsQuery query = new VisitStatsQuery();
        query.setStartDate(today);
        query.setEndDate(today);

        when(hashOperations.get(anyString(), eq("pv"))).thenReturn("8");
        when(hashOperations.get(anyString(), eq("durationCount"))).thenReturn("2");
        when(hashOperations.get(anyString(), eq("durationTotal"))).thenReturn("5000");
        when(hyperLogLogOperations.size(anyString())).thenReturn(3L);

        List<VisitDailyStatsVO> result = visitStatsService.getDailyStats(query);

        assertEquals(1, result.size());
        VisitDailyStatsVO todayStats = result.get(0);
        assertEquals(8L, todayStats.getPv());
        assertEquals(3L, todayStats.getUv());
        assertEquals(2.5, todayStats.getAvgDurationSec());
        verify(blogEventLogMapper, never()).selectDailyStats(any(), any());
    }

    @Test
    void getOverview_shouldCombineDbAndRedis() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        VisitDailyStatsVO dbTotal = new VisitDailyStatsVO();
        dbTotal.setPv(100L);
        dbTotal.setUv(80L);

        VisitDailyStatsVO yesterdayStats = new VisitDailyStatsVO();
        yesterdayStats.setPv(20L);
        yesterdayStats.setUv(15L);

        when(blogEventLogMapper.selectTotalPvUv(today.atStartOfDay().minusNanos(1))).thenReturn(dbTotal);
        when(blogEventLogMapper.selectDayStats(yesterday.atStartOfDay(), yesterday.atTime(LocalTime.MAX)))
                .thenReturn(yesterdayStats);
        when(hashOperations.get(anyString(), eq("pv"))).thenReturn("10");
        when(hashOperations.get(anyString(), eq("durationCount"))).thenReturn("0");
        when(hashOperations.get(anyString(), eq("durationTotal"))).thenReturn("0");
        when(hyperLogLogOperations.size(anyString())).thenReturn(8L);

        VisitOverviewVO overview = visitStatsService.getOverview();

        assertEquals(110L, overview.getTotalPv());
        assertEquals(88L, overview.getTotalUv());
        assertEquals(10L, overview.getTodayPv());
        assertEquals(8L, overview.getTodayUv());
        assertEquals(20L, overview.getYesterdayPv());
        assertEquals(15L, overview.getYesterdayUv());
    }

    @Test
    void getTopPages_shouldMergeTodayRedisData() {
        LocalDate today = LocalDate.now();
        VisitStatsQuery query = new VisitStatsQuery();
        query.setStartDate(today.minusDays(6));
        query.setEndDate(today);
        query.setTopN(5);

        VisitTopPageVO page = new VisitTopPageVO();
        page.setPath("/article/1");
        page.setTitle("旧文章");
        page.setPv(5L);
        page.setUv(3L);

        when(blogEventLogMapper.selectTopPages(any(LocalDateTime.class), any(LocalDateTime.class), eq(5)))
                .thenReturn(Collections.singletonList(page));

        Map<Object, Object> todayPages = new HashMap<>();
        todayPages.put("/article/2", "10");
        todayPages.put("/article/1", "3");
        when(hashOperations.entries(anyString())).thenReturn(todayPages);

        List<VisitTopPageVO> result = visitStatsService.getTopPages(query);

        assertEquals(2, result.size());
        assertEquals("/article/2", result.get(0).getPath());
        assertEquals(10L, result.get(0).getPv());
        assertEquals("/article/1", result.get(1).getPath());
        assertEquals(8L, result.get(1).getPv());
    }

    @Test
    void getDeviceDistribution_shouldMergeTodayRedisData() {
        LocalDate today = LocalDate.now();
        VisitStatsQuery query = new VisitStatsQuery();
        query.setStartDate(today.minusDays(6));
        query.setEndDate(today);

        VisitDeviceVO pc = new VisitDeviceVO();
        pc.setDevice("PC");
        pc.setCount(5L);

        when(blogEventLogMapper.selectDeviceDistribution(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(pc));

        Map<Object, Object> todayDevices = new HashMap<>();
        todayDevices.put("PC", "3");
        todayDevices.put("Mobile", "7");
        when(hashOperations.entries(anyString())).thenReturn(todayDevices);

        List<VisitDeviceVO> result = visitStatsService.getDeviceDistribution(query);

        assertEquals(2, result.size());
        assertEquals("PC", result.get(0).getDevice());
        assertEquals(8L, result.get(0).getCount());
        assertEquals("Mobile", result.get(1).getDevice());
        assertEquals(7L, result.get(1).getCount());
    }
}
