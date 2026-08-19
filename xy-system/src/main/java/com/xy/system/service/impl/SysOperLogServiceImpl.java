package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysOperLog;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysOperLogMapper;
import com.xy.system.service.ISysOperLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 * */
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements ISysOperLogService {

    @Override
    public PageResult<SysOperLog> selectOperLogPage(SysOperLog operLog) {
        int pageNum = operLog.getPageNum() <= 0 ? 1 : operLog.getPageNum();
        int pageSize = operLog.getPageSize() <= 0 ? 10 : operLog.getPageSize();

        LambdaQueryWrapper<SysOperLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(operLog.getTitle())) {
            queryWrapper.like(SysOperLog::getTitle, operLog.getTitle());
        }
        if (StringUtils.isNotEmpty(operLog.getOperName())) {
            queryWrapper.like(SysOperLog::getOperName, operLog.getOperName());
        }
        if (operLog.getBusinessType() != null) {
            queryWrapper.eq(SysOperLog::getBusinessType, operLog.getBusinessType());
        }
        if (operLog.getStatus() != null) {
            queryWrapper.eq(SysOperLog::getStatus, operLog.getStatus());
        }
        queryWrapper.orderByDesc(SysOperLog::getOperTime);

        Page<SysOperLog> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public void cleanOperLog() {
        this.remove(new LambdaQueryWrapper<>());
    }
}
