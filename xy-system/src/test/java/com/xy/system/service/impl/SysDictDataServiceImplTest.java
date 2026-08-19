package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysDictData;
import com.xy.common.entity.SysDictType;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.system.mapper.SysDictDataMapper;
import com.xy.system.mapper.SysDictTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 字典数据服务单元测试
 * */
@ExtendWith(MockitoExtension.class)
class SysDictDataServiceImplTest {

    @Mock
    private SysDictDataMapper dictDataMapper;

    @Mock
    private SysDictTypeMapper dictTypeMapper;

    private SysDictDataServiceImpl dictDataService;

    @BeforeEach
    void setUp() {
        SysDictTypeServiceImpl dictTypeService = new SysDictTypeServiceImpl();
        ReflectionTestUtils.setField(dictTypeService, "baseMapper", dictTypeMapper);

        dictDataService = new SysDictDataServiceImpl(dictTypeService);
        ReflectionTestUtils.setField(dictDataService, "baseMapper", dictDataMapper);
    }

    @Test
    void selectDictDataList_shouldReturnPageResult() {
        SysDictData query = new SysDictData();
        query.setDictType("sys_user_status");
        query.setDictLabel("正常");
        query.setDictValue("0");
        query.setStatus("0");
        query.setPageNum(1);
        query.setPageSize(10);

        SysDictData result = new SysDictData();
        result.setDictCode(1L);
        result.setDictType("sys_user_status");
        result.setDictLabel("正常");
        result.setDictValue("0");

        Page<SysDictData> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(result));

        when(dictDataMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<SysDictData> pageResult = dictDataService.selectDictDataList(query);

        assertNotNull(pageResult);
        assertEquals(1, pageResult.getTotal());
        assertEquals("正常", pageResult.getList().get(0).getDictLabel());
    }

    @Test
    void selectDictDataByType_shouldReturnList() {
        SysDictData result = new SysDictData();
        result.setDictCode(1L);
        result.setDictType("sys_user_status");
        result.setDictLabel("正常");

        when(dictDataMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(result));

        var list = dictDataService.selectDictDataByType("sys_user_status");

        assertEquals(1, list.size());
        assertEquals("正常", list.get(0).getDictLabel());
    }

    @Test
    void selectDictDataById_shouldReturnEntity() {
        SysDictData data = new SysDictData();
        data.setDictCode(1L);
        data.setDictLabel("正常");

        when(dictDataMapper.selectById(1L)).thenReturn(data);

        SysDictData result = dictDataService.selectDictDataById(1L);

        assertNotNull(result);
        assertEquals("正常", result.getDictLabel());
    }

    @Test
    void insertDictData_shouldSuccess() {
        SysDictData data = new SysDictData();
        data.setDictType("sys_user_status");
        data.setDictLabel("正常");
        data.setDictValue("0");

        SysDictType type = new SysDictType();
        type.setDictId(1L);
        type.setDictType("sys_user_status");

        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(type);
        when(dictDataMapper.insert(any(SysDictData.class))).thenAnswer(invocation -> {
            SysDictData arg = invocation.getArgument(0);
            arg.setDictCode(1L);
            return 1;
        });

        Long dictCode = dictDataService.insertDictData(data);

        assertEquals(1L, dictCode);
        assertEquals("0", data.getStatus());
        assertEquals("N", data.getIsDefault());
    }

    @Test
    void insertDictData_shouldThrow_whenDictTypeNotExist() {
        SysDictData data = new SysDictData();
        data.setDictType("sys_not_exist");

        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> dictDataService.insertDictData(data));
        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void updateDictData_shouldSuccess() {
        SysDictData existing = new SysDictData();
        existing.setDictCode(1L);
        existing.setDictType("sys_user_status");

        SysDictData update = new SysDictData();
        update.setDictCode(1L);
        update.setDictType("sys_user_status");
        update.setDictLabel("禁用");

        SysDictType type = new SysDictType();
        type.setDictType("sys_user_status");

        when(dictDataMapper.selectById(1L)).thenReturn(existing);
        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(type);
        when(dictDataMapper.updateById(any(SysDictData.class))).thenReturn(1);

        assertTrue(dictDataService.updateDictData(update));
    }

    @Test
    void deleteDictDataById_shouldSuccess() {
        SysDictData existing = new SysDictData();
        existing.setDictCode(1L);

        when(dictDataMapper.selectById(1L)).thenReturn(existing);
        when(dictDataMapper.deleteById(1L)).thenReturn(1);

        assertTrue(dictDataService.deleteDictDataById(1L));
    }

    @Test
    void deleteDictDataById_shouldThrow_whenNotFound() {
        when(dictDataMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> dictDataService.deleteDictDataById(1L));
        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }
}
