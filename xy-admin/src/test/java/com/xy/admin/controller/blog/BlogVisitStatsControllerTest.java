package com.xy.admin.controller.blog;

import com.xy.common.dto.blog.VisitStatsQuery;
import com.xy.common.vo.blog.VisitDailyStatsVO;
import com.xy.common.vo.blog.VisitDeviceVO;
import com.xy.common.vo.blog.VisitOverviewVO;
import com.xy.common.vo.blog.VisitTopPageVO;
import com.xy.common.vo.blog.VisitTopSourceVO;
import com.xy.system.service.blog.IVisitStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 博客访问统计管理接口测试
 */
@ExtendWith(MockitoExtension.class)
class BlogVisitStatsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IVisitStatsService visitStatsService;

    @InjectMocks
    private BlogVisitStatsController blogVisitStatsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(blogVisitStatsController).build();
    }

    @Test
    void overview_shouldReturnData() throws Exception {
        VisitOverviewVO vo = new VisitOverviewVO();
        vo.setTodayPv(10L);
        vo.setTodayUv(5L);
        when(visitStatsService.getOverview()).thenReturn(vo);

        mockMvc.perform(get("/blog/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.todayPv").value(10));
    }

    @Test
    void daily_shouldReturnData() throws Exception {
        VisitDailyStatsVO vo = new VisitDailyStatsVO();
        vo.setPv(20L);
        vo.setUv(10L);
        when(visitStatsService.getDailyStats(any(VisitStatsQuery.class)))
                .thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/blog/stats/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].pv").value(20));
    }

    @Test
    void topPages_shouldReturnData() throws Exception {
        VisitTopPageVO vo = new VisitTopPageVO();
        vo.setPath("/article/1");
        vo.setPv(15L);
        when(visitStatsService.getTopPages(any(VisitStatsQuery.class)))
                .thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/blog/stats/top/pages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].path").value("/article/1"));
    }

    @Test
    void topSources_shouldReturnData() throws Exception {
        VisitTopSourceVO vo = new VisitTopSourceVO();
        vo.setReferrer("direct");
        vo.setPv(8L);
        when(visitStatsService.getTopSources(any(VisitStatsQuery.class)))
                .thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/blog/stats/top/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].referrer").value("direct"));
    }

    @Test
    void devices_shouldReturnData() throws Exception {
        VisitDeviceVO vo = new VisitDeviceVO();
        vo.setDevice("PC");
        vo.setCount(12L);
        when(visitStatsService.getDeviceDistribution(any(VisitStatsQuery.class)))
                .thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/blog/stats/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].device").value("PC"));
    }
}
