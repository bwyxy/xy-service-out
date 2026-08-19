package com.xy.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysLogininfor;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 登录日志服务接口
 * */
public interface ISysLogininforService extends IService<SysLogininfor> {

    /**
     * 分页查询登录日志
     * @param logininfor 查询条件
     * @return 分页结果
     * */
    PageResult<SysLogininfor> selectLogininforPage(SysLogininfor logininfor);

    /**
     * 根据ID删除登录日志
     * @param infoId 访问ID
     * */
    void deleteLogininforById(Long infoId);

    /**
     * 清空登录日志
     * */
    void cleanLogininfor();

    /**
     * 导出登录日志
     * @param logininfor 查询条件
     * @param response 响应对象
     * */
    void exportLogininfor(SysLogininfor logininfor, HttpServletResponse response);
}
