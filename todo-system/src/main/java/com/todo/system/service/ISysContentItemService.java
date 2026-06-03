package com.todo.system.service;

import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysContentItem;

import java.util.List;

public interface ISysContentItemService {

    /**
     * 分页查询内容通用项
     * */
    public PageResult<SysContentItem> selectContentItems(SysContentItem sysContentItem);

    /**
     * 根据内容配置 Id查询详情
     * */
    public SysContentItem selectContentItemById(Long id);

    /**
     * 添加内容通用项
     * */
    public void addContentItem(SysContentItem sysContentItem);

    /**
     * 更新内容通用项
     * */
    public void updateContentItem(SysContentItem sysContentItem);

    /**
     * 根据内容配置 Id删除
     * */
    public void deleteContentItemById(Long id);
}
