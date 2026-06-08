package com.todo.system.service;

import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysConfig;

public interface ISysConfigService {

    /**
     * 分页查询内容配置列表
     * */
    public PageResult<SysConfig> findSysConfigs(SysConfig sysConfig);

    /**
     * 根据配置 ID查询配置信息
     * */
    public SysConfig findSysConfigById(Long id);

    /**
     * 添加配置
     * */
    public void saveSysConfig(SysConfig sysConfig);

    /**
     * 更新配置
     * */
    public void updateSysConfig(SysConfig sysConfig);

    /**
     * 删除配置
     * */
    public void deleteSysConfigById(Long id);

}
