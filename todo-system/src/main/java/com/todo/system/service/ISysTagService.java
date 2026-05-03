package com.todo.system.service;

import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysTag;

public interface ISysTagService {

    /**
     * 分页查询标签列表
     * */
    public PageResult<SysTag> selectSysTagList(SysTag sysTag);

    /**
     * 根据标签 Id查询标签信息
     * */
    public SysTag selectSysTagById(Long id);

    /**
     * 新增标签
     * */
    public void insertSysTag(SysTag sysTag);

    /**
     * 更新标签信息
     * */
    public void updateSysTag(SysTag sysTag);

    /**
     * 删除标签
     * */
    public void deleteSysTagById(Long id);
}
