package com.xy.system.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.ai.AiModelProviderPageQuery;
import com.xy.common.dto.ai.AiModelProviderRequest;
import com.xy.common.entity.ai.AiModelProvider;
import com.xy.common.exception.ServiceException;
import com.xy.common.vo.ai.AiModelProviderVo;
import com.xy.system.mapper.ai.AiModelProviderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AI 模型提供者后台管理服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class SysAiModelAdminServiceImplTest {

    @Mock
    private AiModelProviderMapper providerMapper;

    @InjectMocks
    private SysAiModelAdminServiceImpl modelAdminService;

    @Test
    void pageProviders_shouldReturnPagedResult() {
        AiModelProviderPageQuery query = new AiModelProviderPageQuery();
        query.setKeyword("OpenAI");
        query.setPageNum(1);
        query.setPageSize(10);

        AiModelProvider provider = createProvider(1L, "OpenAI", "openai", 1);
        Page<AiModelProvider> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(provider));

        when(providerMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<AiModelProviderVo> result = modelAdminService.pageProviders(query);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("OpenAI", result.getList().get(0).getName());
    }

    @Test
    void getProviderById_shouldReturnProviderWhenExists() {
        AiModelProvider provider = createProvider(1L, "OpenAI", "openai", 1);
        when(providerMapper.selectById(1L)).thenReturn(provider);

        AiModelProviderVo result = modelAdminService.getProviderById(1L);

        assertNotNull(result);
        assertEquals("OpenAI", result.getName());
    }

    @Test
    void getProviderById_shouldThrowWhenNotExists() {
        when(providerMapper.selectById(1L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> modelAdminService.getProviderById(1L));
        assertTrue(exception.getMessage().contains("不存在"));
    }

    @Test
    void saveOrUpdateProvider_shouldInsertWhenIdIsNull() {
        AiModelProviderRequest request = new AiModelProviderRequest();
        request.setName("DeepSeek");
        request.setCode("deepseek");
        request.setType("OPENAI_COMPATIBLE");

        when(providerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(providerMapper.insert(any(AiModelProvider.class))).thenReturn(1);

        R<Void> result = modelAdminService.saveOrUpdateProvider(request);

        assertTrue(result.isOk());
        verify(providerMapper, times(1)).insert(any(AiModelProvider.class));
    }

    @Test
    void saveOrUpdateProvider_shouldThrowWhenCodeExists() {
        AiModelProviderRequest request = new AiModelProviderRequest();
        request.setName("DeepSeek");
        request.setCode("deepseek");
        request.setType("OPENAI_COMPATIBLE");

        when(providerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> modelAdminService.saveOrUpdateProvider(request));
        assertTrue(exception.getMessage().contains("已存在"));
    }

    @Test
    void deleteProvider_shouldRemoveById() {
        when(providerMapper.deleteById(1L)).thenReturn(1);

        R<Void> result = modelAdminService.deleteProvider(1L);

        assertTrue(result.isOk());
        verify(providerMapper, times(1)).deleteById(1L);
    }

    @Test
    void toggleProvider_shouldSwitchEnabledStatus() {
        AiModelProvider provider = createProvider(1L, "OpenAI", "openai", 1);
        when(providerMapper.selectById(1L)).thenReturn(provider);
        when(providerMapper.updateById(any(AiModelProvider.class))).thenReturn(1);

        R<Void> result = modelAdminService.toggleProvider(1L);

        assertTrue(result.isOk());
        assertEquals(0, provider.getEnabled());
    }

    private AiModelProvider createProvider(Long id, String name, String code, Integer enabled) {
        AiModelProvider provider = new AiModelProvider();
        provider.setId(id);
        provider.setName(name);
        provider.setCode(code);
        provider.setType("OPENAI_COMPATIBLE");
        provider.setEnabled(enabled);
        provider.setPriority(10);
        return provider;
    }
}
