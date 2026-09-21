package com.xy.blog.controller;

import com.alibaba.fastjson2.JSON;
import com.xy.blog.pojo.dto.BlogVisitTrackDTO;
import com.xy.blog.service.IVisitStatsService;
import com.xy.blog.service.VisitAntiSpamService;
import com.xy.common.vo.blog.VisitOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 访问统计接口控制器测试
 */
@ExtendWith(MockitoExtension.class)
class VisitStatsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IVisitStatsService visitStatsService;

    @Mock
    private VisitAntiSpamService antiSpamService;

    @InjectMocks
    private VisitStatsController visitStatsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(visitStatsController).build();
    }

    @Test
    void track_shouldReturnOkWhenAllowed() throws Exception {
        when(antiSpamService.isAllowed(any(), any())).thenReturn(true);
        doNothing().when(visitStatsService).track(any(), any(), any());

        BlogVisitTrackDTO dto = new BlogVisitTrackDTO();
        dto.setEventType("page_view");
        dto.setPath("/article/1");

        mockMvc.perform(post("/web/stats/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void track_shouldReturnOkWhenBlocked() throws Exception {
        when(antiSpamService.isAllowed(any(), any())).thenReturn(false);

        BlogVisitTrackDTO dto = new BlogVisitTrackDTO();
        dto.setEventType("page_view");
        dto.setPath("/article/1");

        mockMvc.perform(post("/web/stats/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void overview_shouldReturnData() throws Exception {
        VisitOverviewVO vo = new VisitOverviewVO();
        vo.setTodayPv(10L);
        vo.setTodayUv(5L);
        when(visitStatsService.getOverview()).thenReturn(vo);

        mockMvc.perform(get("/web/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.todayPv").value(10));
    }
}
