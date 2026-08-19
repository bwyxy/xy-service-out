package com.xy.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户与角色关联 Mapper
 * */
@Mapper
public interface SysUserRoleMapper {

    /**
     * 根据用户ID查询角色ID列表
     * @param userId 用户ID
     * @return 角色ID列表
     * */
    List<Long> selectRoleIdsByUserId(Long userId);

    /**
     * 批量新增用户角色关联
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * */
    void batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 根据用户ID删除角色关联
     * @param userId 用户ID
     * */
    void deleteByUserId(Long userId);

    /**
     * 根据角色ID统计关联用户数
     * @param roleId 角色ID
     * @return 用户数
     * */
    long countByRoleId(Long roleId);
}
