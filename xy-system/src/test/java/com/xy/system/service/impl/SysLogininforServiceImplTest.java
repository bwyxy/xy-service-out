package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysLogininfor;
import com.xy.system.mapper.SysLogininforMapper;
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
 * 登录日志服务单元测试
 * */
@ExtendWith(MockitoExtension.class)
class SysLogininforServiceImplTest {

    @Mock
    private SysLogininforMapper logininforMapper;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private SysLogininforServiceImpl logininforService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(logininforService, "baseMapper", logininforMapper);
    }

    @Test
    void selectLogininforPage_shouldReturnPageResult() {
        SysLogininfor query = new SysLogininfor();
        query.setUserName("admin");
        query.setIpaddr("127.0.0.1");
        query.setStatus("0");
        query.setPageNum(1);
        query.setPageSize(10);

        SysLogininfor info = new SysLogininfor();
        info.setInfoId(1L);
        info.setUserName("admin");
        info.setIpaddr("127.0.0.1");
        info.setStatus("0");
        info.setLoginTime(LocalDateTime.now());

        Page<SysLogininfor> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(info));

        when(logininforMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<SysLogininfor> pageResult = logininforService.selectLogininforPage(query);

        assertNotNull(pageResult);
        assertEquals(1, pageResult.getTotal());
        assertEquals("admin", pageResult.getList().get(0).getUserName());
    }

    @Test
    void selectLogininforPage_shouldHandleTimeParams() {
        SysLogininfor query = new SysLogininfor();
        query.setPageNum(1);
        query.setPageSize(10);
        Map<String, Object> params = new HashMap<>();
        params.put("beginTime", "2026-08-01 00:00:00");
        params.put("endTime", "2026-08-20 23:59:59");
        query.setParams(params);

        Page<SysLogininfor> page = new Page<>(1, 10, 0);
        page.setRecords(Collections.emptyList());

        when(logininforMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<SysLogininfor> pageResult = logininforService.selectLogininforPage(query);

        assertNotNull(pageResult);
        assertEquals(0, pageResult.getTotal());
    }

    @Test
    void deleteLogininforById_shouldRemove() {
        when(logininforMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> logininforService.deleteLogininforById(1L));

        verify(logininforMapper).deleteById(1L);
    }

    @Test
    void cleanLogininfor_shouldRemoveAll() {
        when(logininforMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(10);

        assertDoesNotThrow(() -> logininforService.cleanLogininfor());

        verify(logininforMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void exportLogininfor_shouldWriteResponse() throws IOException {
        SysLogininfor query = new SysLogininfor();

        SysLogininfor info = new SysLogininfor();
        info.setInfoId(1L);
        info.setUserName("admin");
        info.setIpaddr("127.0.0.1");
        info.setLoginLocation("本地");
        info.setBrowser("Chrome");
        info.setOs("Windows");
        info.setStatus("0");
        info.setMsg("登录成功");
        info.setLoginTime(LocalDateTime.now());

        when(logininforMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(info));
        when(response.getOutputStream()).thenReturn(new TestServletOutputStream());

        assertDoesNotThrow(() -> logininforService.exportLogininfor(query, response));

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
