package com.todo.app.service;

import com.todo.common.entity.SysConfig;

import java.util.List;

public interface IConfigService {

    /**
     * 查询底部导航栏
     * */
    public List<SysConfig> selectTabbarList();

    /**
     * 查询工具配置
     * */
    public List<SysConfig> selectScheduleList();

    /**
     * 查询用户菜单配置
     * */
    public List<SysConfig> selectUserMenuList();

    /**
     * 查询轮播图配置
     * */
    public List<SysConfig> selectSwiperList();
}
