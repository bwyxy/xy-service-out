package com.xy.system.service.impl;

import com.xy.common.entity.SysRole;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.system.mapper.SysRoleMapper;
import com.xy.system.service.ISysRoleMenuService;
import com.xy.system.service.ISysUserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 角色服务单元测试
 * */
@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private ISysRoleMenuService roleMenuService;

    @Mock
    private ISysUserRoleService userRoleService;

    @InjectMocks
    private SysRoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(roleService, "baseMapper", sysRoleMapper);
    }

    @Test
    void insertRole_shouldSaveRoleAndMenus() {
        SysRole role = new SysRole();
        role.setRoleName("测试角色");
        role.setRoleKey("test");
        role.setRoleSort(1);
        role.setMenuIds(List.of(1L, 2L));

        when(sysRoleMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(sysRoleMapper.insert(any(SysRole.class))).thenAnswer(invocation -> {
            SysRole arg = invocation.getArgument(0);
            arg.setId(10L);
            return 1;
        });

        Long roleId = roleService.insertRole(role);

        assertEquals(10L, roleId);
        assertEquals("0", role.getStatus());
        verify(roleMenuService, times(1)).saveRoleMenu(10L, List.of(1L, 2L));
    }

    @Test
    void insertRole_shouldThrowWhenRoleNameExists() {
        SysRole role = new SysRole();
        role.setRoleName("重复角色");
        role.setRoleKey("duplicate");

        SysRole existing = new SysRole();
        existing.setId(2L);
        existing.setRoleName("重复角色");
        when(sysRoleMapper.selectOne(any(), anyBoolean())).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> roleService.insertRole(role));
        assertEquals(ResultCode.DATA_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void updateRole_shouldThrowWhenDisablingSuperAdminRole() {
        SysRole existing = new SysRole();
        existing.setId(1L);
        existing.setRoleName("超级管理员");
        existing.setRoleKey("admin");

        SysRole update = new SysRole();
        update.setId(1L);
        update.setRoleName("超级管理员");
        update.setRoleKey("admin");
        update.setStatus("1");

        when(sysRoleMapper.selectById(1L)).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> roleService.updateRole(update));
        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("停用"));
    }

    @Test
    void deleteRole_shouldThrowWhenDeletingSuperAdminRole() {
        SysRole existing = new SysRole();
        existing.setId(1L);
        existing.setRoleKey("admin");

        when(sysRoleMapper.selectById(1L)).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> roleService.deleteRole(1L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("删除"));
    }

    @Test
    void deleteRole_shouldThrowWhenRoleHasUsers() {
        SysRole existing = new SysRole();
        existing.setId(2L);
        existing.setRoleKey("user");

        when(sysRoleMapper.selectById(2L)).thenReturn(existing);
        when(userRoleService.countUserByRoleId(2L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> roleService.deleteRole(2L));
        assertEquals(ResultCode.BUSINESS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void deleteRole_shouldSucceedForUnusedRole() {
        SysRole existing = new SysRole();
        existing.setId(2L);
        existing.setRoleKey("user");

        when(sysRoleMapper.selectById(2L)).thenReturn(existing);
        when(userRoleService.countUserByRoleId(2L)).thenReturn(0L);
        when(sysRoleMapper.deleteById(2L)).thenReturn(1);

        boolean result = roleService.deleteRole(2L);

        assertTrue(result);
        verify(roleMenuService, times(1)).deleteRoleMenuByRoleId(2L);
    }

    @Test
    void isSuperAdminRole_shouldReturnTrueForAdminKey() {
        SysRole role = new SysRole();
        role.setRoleKey("admin");
        assertTrue(roleService.isSuperAdminRole(role));
    }

    @Test
    void isSuperAdminRole_shouldReturnFalseForOtherKey() {
        SysRole role = new SysRole();
        role.setRoleKey("user");
        assertFalse(roleService.isSuperAdminRole(role));
    }

    @Test
    void checkRoleNameUnique_shouldReturnTrueForNewName() {
        SysRole role = new SysRole();
        role.setRoleName("新角色");

        when(sysRoleMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        assertTrue(roleService.checkRoleNameUnique(role));
    }
}
