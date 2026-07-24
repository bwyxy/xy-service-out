package com.xy.system.service;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysResource;

public interface ISysResourceService {

    /**
     * 分页查询资源列表
     * */
    public PageResult<SysResource> selectSysResourceList(SysResource sysResource);

    /**
     * 新增资源
     * */
    public void addSysResource(SysResource sysResource);

    /**
     * 更新资源 url
     * */
    public void updateResourceUrl(String url);
}
