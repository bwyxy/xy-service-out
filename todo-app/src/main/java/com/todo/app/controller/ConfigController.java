package com.todo.app.controller;

import com.todo.app.service.IConfigService;
import com.todo.common.annotation.Anonymous;
import com.todo.common.core.domain.R;
import com.todo.common.entity.SysConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Anonymous
@RestController
@RequestMapping("/config")
public class ConfigController {

    @Autowired
    private IConfigService configService;

    /**
     * 获取底部导航配置
     * */
    @PostMapping("/tabbar")
    public R<?> getTabbarConfig() {
        List<SysConfig> tabbars = configService.selectTabbarList();

        return R.ok(tabbars);
    }

    /**
     * 获取用户菜单配置
     * */
    @PostMapping("/userMenu")
    public R<?> getUserMenu() {
        List<SysConfig> userMenu = configService.selectUserMenuList();

        return R.ok(userMenu);
    }

    /**
     * 获取工具配置
     * */
    @PostMapping("/utils")
    public R<?> getUtils() {
        List<SysConfig> utils = configService.selectScheduleList();

        return R.ok(utils);
    }

    /**
     * 获取轮播图配置
     * */
    @PostMapping("/swiper")
    public R<?> getSwiper() {
        List<SysConfig> swiper = configService.selectSwiperList();
        return R.ok(swiper);
    }
}
