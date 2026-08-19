package com.xy.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysOperLog;

/**
 * 操作日志服务接口
 * */
public interface ISysOperLogService extends IService<SysOperLog> {

    /**
     * 分页查询操作日志
     * @param operLog 查询条件
     * @return 分页结果
     * */
    PageResult<SysOperLog> selectOperLogPage(SysOperLog operLog);

    /**
     * 清空操作日志
     * */
    void cleanOperLog();
}
