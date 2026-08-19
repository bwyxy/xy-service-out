package com.xy.system.service.impl;

import com.xy.common.entity.SysRole;
import com.xy.common.entity.SysUser;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.system.mapper.SysUserMapper;
import com.xy.system.service.ISysRoleService;
import com.xy.system.service.ISysUserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 系统用户服务单元测试
 * */
@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private ISysRoleService roleService;

    @Mock
    private ISysUserRoleService userRoleService;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sysUserService, "baseMapper", sysUserMapper);
    }

    @Test
    void insertUser_shouldEncryptPasswordAndReturnUserId() {
        SysUser user = new SysUser();
        user.setUsername("testUser");
        user.setPassword("123456");

        when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser arg = invocation.getArgument(0);
            arg.setId(100L);
            return 1;
        });

        Long userId = sysUserService.insertUser(user);

        assertEquals(100L, userId);
        assertNotEquals("123456", user.getPassword());
        assertTrue(sysUserService.checkPassword("123456", user.getPassword()));
        verify(userRoleService, never()).saveUserRole(any(), any());
    }

    @Test
    void insertUser_shouldSaveRoleIds() {
        SysUser user = new SysUser();
        user.setUsername("roleUser");
        user.setPassword("123456");
        user.setRoleIds(List.of(1L, 2L));

        when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser arg = invocation.getArgument(0);
            arg.setId(101L);
            return 1;
        });

        Long userId = sysUserService.insertUser(user);

        assertEquals(101L, userId);
        verify(userRoleService, times(1)).saveUserRole(101L, List.of(1L, 2L));
    }

    @Test
    void insertUser_shouldThrowWhenUsernameExists() {
        SysUser user = new SysUser();
        user.setUsername("existingUser");

        SysUser existing = new SysUser();
        existing.setId(2L);
        existing.setUsername("existingUser");
        when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.insertUser(user));
        assertEquals(ResultCode.DATA_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void updateUser_shouldEncryptPasswordWhenProvided() {
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setUsername("oldName");
        existing.setPassword("$2a$10$oldEncodedPassword");

        SysUser update = new SysUser();
        update.setId(1L);
        update.setUsername("oldName");
        update.setPassword("newPass");
        update.setStatus("0");
        update.setRoleIds(List.of(2L));

        when(sysUserMapper.selectById(1L)).thenReturn(existing);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);
        when(roleService.selectRolesByUserId(1L)).thenReturn(List.of(createRole("user")));

        boolean result = sysUserService.updateUser(update);

        assertTrue(result);
        assertNotEquals("newPass", update.getPassword());
        assertTrue(sysUserService.checkPassword("newPass", update.getPassword()));
        verify(userRoleService, times(1)).saveUserRole(1L, List.of(2L));
    }

    @Test
    void updateUser_shouldKeepPasswordWhenEmpty() {
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setUsername("name");
        existing.setPassword("$2a$10$oldEncodedPassword");

        SysUser update = new SysUser();
        update.setId(1L);
        update.setUsername("name");
        update.setPassword("");
        update.setStatus("0");

        when(sysUserMapper.selectById(1L)).thenReturn(existing);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);
        when(roleService.selectRolesByUserId(1L)).thenReturn(List.of(createRole("user")));

        boolean result = sysUserService.updateUser(update);

        assertTrue(result);
        assertNull(update.getPassword());
    }

    @Test
    void updateUser_shouldThrowWhenDisablingSuperAdmin() {
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setUsername("admin");

        SysUser update = new SysUser();
        update.setId(1L);
        update.setStatus("1");

        when(sysUserMapper.selectById(1L)).thenReturn(existing);
        when(roleService.selectRolesByUserId(1L)).thenReturn(List.of(createRole("admin")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.updateUser(update));
        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("停用"));
    }

    @Test
    void deleteUser_shouldThrowWhenDeletingSuperAdmin() {
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setUsername("admin");

        when(sysUserMapper.selectById(1L)).thenReturn(existing);
        when(roleService.selectRolesByUserId(1L)).thenReturn(List.of(createRole("admin")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.deleteUser(1L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("删除"));
        verify(sysUserMapper, never()).deleteById(any());
    }

    @Test
    void deleteUser_shouldSucceedForNormalUser() {
        SysUser existing = new SysUser();
        existing.setId(2L);
        existing.setUsername("normalUser");

        when(sysUserMapper.selectById(2L)).thenReturn(existing);
        when(roleService.selectRolesByUserId(2L)).thenReturn(List.of(createRole("user")));
        when(sysUserMapper.deleteById(2L)).thenReturn(1);

        boolean result = sysUserService.deleteUser(2L);

        assertTrue(result);
        verify(userRoleService, times(1)).deleteUserRoleByUserId(2L);
    }

    @Test
    void checkUsernameUnique_shouldReturnTrueForNewUsername() {
        SysUser user = new SysUser();
        user.setUsername("uniqueName");

        when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        assertTrue(sysUserService.checkUsernameUnique(user));
    }

    @Test
    void isSuperAdmin_shouldReturnTrueForAdminRole() {
        when(roleService.selectRolesByUserId(1L)).thenReturn(List.of(createRole("admin")));
        assertTrue(sysUserService.isSuperAdmin(1L));
    }

    @Test
    void isSuperAdmin_shouldReturnFalseForNormalRole() {
        when(roleService.selectRolesByUserId(2L)).thenReturn(List.of(createRole("user")));
        assertFalse(sysUserService.isSuperAdmin(2L));
    }

    @Test
    void isSuperAdmin_shouldReturnFalseWhenNoRoles() {
        when(roleService.selectRolesByUserId(3L)).thenReturn(Collections.emptyList());
        assertFalse(sysUserService.isSuperAdmin(3L));
    }

    private SysRole createRole(String roleKey) {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleName(roleKey);
        role.setRoleKey(roleKey);
        return role;
    }
}
