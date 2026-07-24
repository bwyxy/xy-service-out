package com.xy.blog.service;

import com.xy.common.entity.SysTag;

import java.util.List;

/**
 * 标签 逻辑层
 * */
public interface ITagService {

    /**
     * 获取标签列表
     * */
    public List<SysTag> getSysTagList();

    /**
     * 获取标签列表(根据技能级别)
     * */
    public List<SysTag> getSysTagByLevel();
}
