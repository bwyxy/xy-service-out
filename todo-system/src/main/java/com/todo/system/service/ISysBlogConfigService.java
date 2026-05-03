package com.todo.system.service;

import com.todo.common.entity.SysBlogConfig;

import java.util.List;

public interface ISysBlogConfigService {

    /**
     * 查询博客配置列表
     * */
    public List<SysBlogConfig> selectBlogConfigList(SysBlogConfig sysBlogConfig);

    /**
     * 根据配置 ID 查询博客配置信息
     * */
    public SysBlogConfig selectBlogConfigById(Long id);

    /**
     * 新增配置信息
     * */
    public void saveBlogConfig(SysBlogConfig sysBlogConfig);

    /**
     * 更新配置信息
     * */
    public void updateBlogConfig(SysBlogConfig sysBlogConfig);
}
