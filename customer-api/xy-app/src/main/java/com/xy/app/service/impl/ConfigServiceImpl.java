package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.app.service.IConfigService;
import com.xy.common.entity.SysConfig;
import com.xy.common.enums.ConfigType;
import com.xy.system.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统配置 逻辑层
 * */
@Service
public class ConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements IConfigService {

    /**
     * 查询底部导航栏
     * */
    @Override
    public List<SysConfig> selectTabbarList() {
        ConfigType configType = ConfigType.tabbar;

        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysConfig::getConfigType, configType)
                .eq(SysConfig::getDeleted, 0)
                .eq(SysConfig::getStatus, 1);

        return list(queryWrapper);
    }

    /**
     * 查询工具配置
     * */
    @Override
    public List<SysConfig> selectScheduleList() {
        ConfigType configType = ConfigType.utils;

        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysConfig::getConfigType, configType)
                .eq(SysConfig::getDeleted, 0)
                .eq(SysConfig::getStatus, 1);

        return list(queryWrapper);
    }

    /**
     * 查询用户菜单配置
     * */
    @Override
    public List<SysConfig> selectUserMenuList() {
        ConfigType configType = ConfigType.userMenu;

        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysConfig::getConfigType, configType)
                .eq(SysConfig::getDeleted, 0)
                .eq(SysConfig::getStatus, 1);

        return list(queryWrapper);
    }

    /**
     * 查询轮播图配置
     * */
    @Override
    public List<SysConfig> selectSwiperList() {
        ConfigType configType = ConfigType.swiper;

        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysConfig::getConfigType, configType)
                .eq(SysConfig::getDeleted, 0)
                .eq(SysConfig::getStatus, 1);

        return list(queryWrapper);
    }
}
