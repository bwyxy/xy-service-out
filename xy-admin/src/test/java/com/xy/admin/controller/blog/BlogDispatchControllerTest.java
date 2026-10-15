package com.xy.admin.controller.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysRole;
import com.xy.common.entity.blog.BlogDispatchArticleStat;
import com.xy.common.vo.blog.DispatchPlatformVO;
import com.xy.common.vo.blog.DispatchStatSummaryVO;
import com.xy.common.vo.blog.DispatchSubmitVO;
import com.xy.common.vo.blog.DispatchTaskVO;
import com.xy.common.vo.blog.DispatchTestResultVO;
import com.xy.system.service.ISysRoleService;
import com.xy.system.service.blog.IBlogDispatchPlatformService;
import com.xy.system.service.blog.IBlogDispatchStatService;
import com.xy.system.service.blog.IBlogDispatchTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 博客多平台分发管理接口测试
 * 覆盖：平台配置增删改查与连通性测试、单篇/批量分发提交、任务重试与删除、运营数据查询与同步
 * */
@ExtendWith(MockitoExtension.class)
class BlogDispatchControllerTest {

    @Mock
    private IBlogDispatchPlatformService platformService;

    @Mock
    private IBlogDispatchTaskService taskService;

    @Mock
    private IBlogDispatchStatService statService;

    @Mock
    private ISysRoleService roleService;

    @InjectMocks
    private BlogDispatchPlatformController platformController;

    @InjectMocks
    private BlogDispatchTaskController taskController;

    @InjectMocks
    private BlogDispatchStatController statController;

    private MockMvc platformMockMvc;

    private MockMvc taskMockMvc;

    private MockMvc statMockMvc;

    @BeforeEach
    void setUp() {
        platformMockMvc = MockMvcBuilders.standaloneSetup(platformController).build();
        taskMockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
        statMockMvc = MockMvcBuilders.standaloneSetup(statController).build();
    }

    @Test
    void platformList_shouldReturnMaskedCredentialState() throws Exception {
        DispatchPlatformVO vo = new DispatchPlatformVO();
        vo.setId(1L);
        vo.setPlatformCode("juejin");
        vo.setPlatformName("掘金");
        vo.setEnabled(1);
        vo.setCredentialConfigured(true);
        vo.setAdapterSupported(true);
        when(platformService.selectPlatformList()).thenReturn(List.of(vo));

        platformMockMvc.perform(get("/blog/dispatch/platform/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].platformName").value("掘金"))
                .andExpect(jsonPath("$.data[0].credentialConfigured").value(true))
                .andExpect(jsonPath("$.data[0].credentialCipher").doesNotExist());
    }

    @Test
    void supportedPlatforms_shouldReturnAdapterCapability() throws Exception {
        DispatchPlatformVO vo = new DispatchPlatformVO();
        vo.setPlatformCode("wechat");
        vo.setPlatformName("微信公众平台");
        vo.setRequiredCredentialKeys(List.of("appId", "appSecret"));
        when(platformService.selectSupportedPlatforms()).thenReturn(List.of(vo));

        platformMockMvc.perform(get("/blog/dispatch/platform/supported"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].platformCode").value("wechat"))
                .andExpect(jsonPath("$.data[0].requiredCredentialKeys[0]").value("appId"));
    }

    @Test
    void addPlatform_shouldReturnSuccess() throws Exception {
        platformMockMvc.perform(post("/blog/dispatch/platform/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platformCode\":\"juejin\",\"platformName\":\"掘金\",\"enabled\":1,"
                                + "\"credentials\":{\"cookie\":\"session=abc\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("新增成功"));

        verify(platformService).addPlatform(any());
    }

    @Test
    void updatePlatform_shouldReturnSuccess() throws Exception {
        platformMockMvc.perform(put("/blog/dispatch/platform/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"platformCode\":\"juejin\",\"platformName\":\"掘金\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("更新成功"));

        verify(platformService).updatePlatform(any());
    }

    @Test
    void testPlatform_shouldReturnTestResult() throws Exception {
        DispatchTestResultVO result = new DispatchTestResultVO();
        result.setSuccess(true);
        result.setMessage("连通性测试通过");
        when(platformService.testConnection(1L)).thenReturn(result);

        platformMockMvc.perform(post("/blog/dispatch/platform/test/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.message").value("连通性测试通过"));
    }

    @Test
    void deletePlatform_shouldReturnSuccess() throws Exception {
        platformMockMvc.perform(delete("/blog/dispatch/platform/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("删除成功"));

        verify(platformService).deletePlatform(1L);
    }

    @Test
    void roles_shouldReturnRoleOptions() throws Exception {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleName("运营");
        when(roleService.list()).thenReturn(List.of(role));

        platformMockMvc.perform(get("/blog/dispatch/platform/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roleName").value("运营"));
    }

    @Test
    void taskList_shouldReturnPagedTasks() throws Exception {
        DispatchTaskVO vo = new DispatchTaskVO();
        vo.setId(1L);
        vo.setArticleTitle("测试文章");
        vo.setPlatformName("掘金");
        vo.setStatus(3);
        vo.setStatusDesc("分发失败");
        vo.setErrorType("RATE_LIMITED");
        vo.setRetryable(true);
        PageResult<DispatchTaskVO> page = PageResult.of(List.of(vo), 1L, 1L, 15L);
        when(taskService.selectTaskPage(any())).thenReturn(page);

        taskMockMvc.perform(post("/blog/dispatch/task/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].statusDesc").value("分发失败"))
                .andExpect(jsonPath("$.data.list[0].retryable").value(true));
    }

    @Test
    void dispatch_shouldReturnCreatedCount() throws Exception {
        DispatchSubmitVO submitVO = new DispatchSubmitVO();
        submitVO.setBatchNo("BD202610151200000001");
        submitVO.setCreatedCount(2);
        submitVO.setSkippedCount(1);
        when(taskService.submitDispatch(any())).thenReturn(submitVO);

        taskMockMvc.perform(post("/blog/dispatch/task/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleIds\":[1,2],\"platformCodes\":[\"juejin\",\"csdn\"],"
                                + "\"ignoreDispatched\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdCount").value(2))
                .andExpect(jsonPath("$.data.batchNo").value("BD202610151200000001"));
    }

    @Test
    void retry_shouldAcceptTaskIdArray() throws Exception {
        taskMockMvc.perform(post("/blog/dispatch/task/retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("已提交重试"));

        verify(taskService).retryTasks(eq(List.of(1L, 2L, 3L)));
    }

    @Test
    void remove_shouldAcceptTaskIdArray() throws Exception {
        taskMockMvc.perform(post("/blog/dispatch/task/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[9]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("删除成功"));

        verify(taskService).deleteTasks(eq(List.of(9L)));
    }

    @Test
    void taskDetail_shouldReturnSingleTask() throws Exception {
        DispatchTaskVO vo = new DispatchTaskVO();
        vo.setId(5L);
        vo.setResultUrl("https://juejin.cn/post/7001");
        when(taskService.selectTaskById(5L)).thenReturn(vo);

        taskMockMvc.perform(get("/blog/dispatch/task/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultUrl").value("https://juejin.cn/post/7001"));
    }

    @Test
    void statSummary_shouldReturnGroupedMetrics() throws Exception {
        DispatchStatSummaryVO summary = new DispatchStatSummaryVO();
        summary.setPlatformCode("juejin");
        summary.setPlatformName("掘金");
        summary.setViewCount(188L);
        when(statService.selectPlatformSummary("juejin")).thenReturn(List.of(summary));

        statMockMvc.perform(get("/blog/dispatch/stat/summary").param("platformCode", "juejin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].platformName").value("掘金"))
                .andExpect(jsonPath("$.data[0].viewCount").value(188));
    }

    @Test
    void statList_shouldReturnPagedMetrics() throws Exception {
        BlogDispatchArticleStat stat = new BlogDispatchArticleStat();
        stat.setId(1L);
        stat.setArticleTitle("分发测试");
        stat.setViewCount(100);
        stat.setCollectCount(3);
        when(statService.selectStatPage(any())).thenReturn(PageResult.of(List.of(stat), 1L, 1L, 10L));

        statMockMvc.perform(post("/blog/dispatch/stat/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].viewCount").value(100))
                .andExpect(jsonPath("$.data.list[0].collectCount").value(3));
    }

    @Test
    void statSync_shouldReturnUpdatedCount() throws Exception {
        when(statService.refreshStats()).thenReturn(2);

        statMockMvc.perform(post("/blog/dispatch/stat/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("同步完成，更新 2 条记录"));
    }
}
