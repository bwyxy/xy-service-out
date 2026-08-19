package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysDictType;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.system.mapper.SysDictTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 字典类型服务单元测试
 * */
@ExtendWith(MockitoExtension.class)
class SysDictTypeServiceImplTest {

    @Mock
    private SysDictTypeMapper dictTypeMapper;

    @InjectMocks
    private SysDictTypeServiceImpl dictTypeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dictTypeService, "baseMapper", dictTypeMapper);
    }

    @Test
    void selectDictTypeList_shouldReturnPageResult() {
        SysDictType query = new SysDictType();
        query.setDictName("用户");
        query.setDictType("sys_user");
        query.setStatus("0");
        query.setPageNum(1);
        query.setPageSize(10);

        SysDictType result = new SysDictType();
        result.setDictId(1L);
        result.setDictName("用户状态");
        result.setDictType("sys_user_status");
        result.setStatus("0");

        Page<SysDictType> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(result));

        when(dictTypeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<SysDictType> pageResult = dictTypeService.selectDictTypeList(query);

        assertNotNull(pageResult);
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertEquals("用户状态", pageResult.getList().get(0).getDictName());
    }

    @Test
    void selectDictTypeById_shouldReturnEntity() {
        SysDictType type = new SysDictType();
        type.setDictId(1L);
        type.setDictName("用户状态");

        when(dictTypeMapper.selectById(1L)).thenReturn(type);

        SysDictType result = dictTypeService.selectDictTypeById(1L);

        assertNotNull(result);
        assertEquals("用户状态", result.getDictName());
    }

    @Test
    void insertDictType_shouldSuccess_whenUnique() {
        SysDictType type = new SysDictType();
        type.setDictName("用户状态");
        type.setDictType("sys_user_status");

        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);
        when(dictTypeMapper.insert(any(SysDictType.class))).thenAnswer(invocation -> {
            SysDictType arg = invocation.getArgument(0);
            arg.setDictId(1L);
            return 1;
        });

        Long dictId = dictTypeService.insertDictType(type);

        assertEquals(1L, dictId);
        assertEquals("0", type.getStatus());
    }

    @Test
    void insertDictType_shouldThrow_whenDuplicate() {
        SysDictType type = new SysDictType();
        type.setDictName("用户状态");
        type.setDictType("sys_user_status");

        SysDictType existing = new SysDictType();
        existing.setDictId(2L);
        existing.setDictType("sys_user_status");

        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class, () -> dictTypeService.insertDictType(type));
        assertEquals(ResultCode.DATA_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void updateDictType_shouldSuccess() {
        SysDictType existing = new SysDictType();
        existing.setDictId(1L);
        existing.setDictType("sys_user_status");

        SysDictType update = new SysDictType();
        update.setDictId(1L);
        update.setDictType("sys_user_status");
        update.setDictName("用户状态更新");

        when(dictTypeMapper.selectById(1L)).thenReturn(existing);
        when(dictTypeMapper.updateById(any(SysDictType.class))).thenReturn(1);

        assertTrue(dictTypeService.updateDictType(update));
    }

    @Test
    void updateDictType_shouldThrow_whenNotFound() {
        SysDictType update = new SysDictType();
        update.setDictId(1L);

        when(dictTypeMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> dictTypeService.updateDictType(update));
        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void deleteDictTypeById_shouldSuccess() {
        SysDictType existing = new SysDictType();
        existing.setDictId(1L);

        when(dictTypeMapper.selectById(1L)).thenReturn(existing);
        when(dictTypeMapper.deleteById(1L)).thenReturn(1);

        assertTrue(dictTypeService.deleteDictTypeById(1L));
    }

    @Test
    void deleteDictTypeById_shouldThrow_whenNotFound() {
        when(dictTypeMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> dictTypeService.deleteDictTypeById(1L));
        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void checkDictTypeUnique_shouldReturnTrue_whenNotExist() {
        SysDictType type = new SysDictType();
        type.setDictType("sys_user_status");

        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        assertTrue(dictTypeService.checkDictTypeUnique(type));
    }

    @Test
    void checkDictTypeUnique_shouldReturnTrue_whenSameId() {
        SysDictType type = new SysDictType();
        type.setDictId(1L);
        type.setDictType("sys_user_status");

        SysDictType existing = new SysDictType();
        existing.setDictId(1L);
        existing.setDictType("sys_user_status");

        when(dictTypeMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

        assertTrue(dictTypeService.checkDictTypeUnique(type));
    }
}
