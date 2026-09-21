package com.xy.system.service;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysCategory;
import com.xy.common.vo.blog.CategoryVO;

import java.util.List;

public interface ISysCategoryService {

    /**
     * 分页查询分类列表
     * */
    public PageResult<SysCategory> selectSysCategoryList(SysCategory sysCategory);

    /**
     * 根据分类 Id查询分类信息
     * */
    public SysCategory selectSysCategoryById(Long id);

    /**
     * 新增分类
     * */
    public void insertSysCategory(SysCategory sysCategory);

    /**
     * 更新分类
     * */
    public void updateSysCategory(SysCategory sysCategory);

    /**
     * 删除分类
     * */
    public void deleteSysCategoryById(Long id);
}
