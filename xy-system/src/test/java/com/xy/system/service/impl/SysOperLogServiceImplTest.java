package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysOperLog;
import com.xy.system.mapper.SysOperLogMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 操作日志服务单元测试
 * */
@ExtendWith(MockitoExtension.class)
class SysOperLogServiceImplTest {

    @Mock
    private SysOperLogMapper operLogMapper;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private SysOperLogServiceImpl operLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(operLogService, "baseMapper", operLogMapper);
    }

    @Test
    void selectOperLogPage_shouldReturnPageResult() {
        SysOperLog query = new SysOperLog();
        query.setTitle("用户管理");
        query.setOperName("admin");
        query.setBusinessType(1);
        query.setStatus(0);
        query.setPageNum(1);
        query.setPageSize(10);

        SysOperLog log = new SysOperLog();
        log.setId(1L);
        log.setTitle("用户管理");
        log.setOperName("admin");
        log.setBusinessType(1);
        log.setStatus(0);
        log.setOperTime(LocalDateTime.now());

        Page<SysOperLog> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(log));

        when(operLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<SysOperLog> pageResult = operLogService.selectOperLogPage(query);

        assertNotNull(pageResult);
        assertEquals(1, pageResult.getTotal());
        assertEquals("admin", pageResult.getList().get(0).getOperName());
    }

    @Test
    void selectOperLogPage_shouldHandleTimeParams() {
        SysOperLog query = new SysOperLog();
        query.setPageNum(1);
        query.setPageSize(10);
        Map<String, Object> params = new HashMap<>();
        params.put("beginTime", "2026-08-01 00:00:00");
        params.put("endTime", "2026-08-20 23:59:59");
        query.setParams(params);

        Page<SysOperLog> page = new Page<>(1, 10, 0);
        page.setRecords(Collections.emptyList());

        when(operLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<SysOperLog> pageResult = operLogService.selectOperLogPage(query);

        assertNotNull(pageResult);
        assertEquals(0, pageResult.getTotal());
    }

    @Test
    void cleanOperLog_shouldRemoveAll() {
        when(operLogMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(10);

        assertDoesNotThrow(() -> operLogService.cleanOperLog());

        verify(operLogMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void exportOperLog_shouldWriteResponse() throws IOException {
        SysOperLog query = new SysOperLog();

        SysOperLog log = new SysOperLog();
        log.setId(1L);
        log.setTitle("用户管理");
        log.setBusinessType(1);
        log.setRequestMethod("POST");
        log.setOperName("admin");
        log.setOperUrl("/system/user");
        log.setOperIp("127.0.0.1");
        log.setOperLocation("本地");
        log.setStatus(0);
        log.setOperTime(LocalDateTime.now());
        log.setCostTime(100L);
        log.setErrorMsg("");

        when(operLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(log));
        when(response.getOutputStream()).thenReturn(new TestServletOutputStream());

        assertDoesNotThrow(() -> operLogService.exportOperLog(query, response));

        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        verify(response).setHeader(anyString(), anyString());
    }

    static class TestServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            outputStream.write(b);
        }

        @Override
        public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
        }

        @Override
        public boolean isReady() {
            return true;
        }
    }
}
