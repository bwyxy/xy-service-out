package com.xy.system.service;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysImUser;

/**
 * 悟空 IM系统账号逻辑层
 * */
public interface ISysImUserService {

    /**
     * 获取列表
     * */
    PageResult<SysImUser> selectSysImUserList(SysImUser sysImUser);

    /**
     * 新增 IM系统账号
     * */
    void insertSysImUser(SysImUser sysImUser);

    /**
     * 更新 IM系统账号
     * */
    void updateSysImUser(SysImUser sysImUser);
}
