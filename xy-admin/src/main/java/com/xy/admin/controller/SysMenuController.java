package com.xy.admin.controller;

import com.xy.common.core.domain.R;
import com.xy.common.entity.SysMenu;
import com.xy.common.utils.SecurityUtils;
import com.xy.system.service.ISysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    @Autowired
    private ISysMenuService sysMenuService;

    /** 查询菜单列表
     * */
    @PostMapping("/list")
    public R<?> list(SysMenu sysMenu) {

        List<SysMenu> menus = sysMenuService.selectMenuList(sysMenu, SecurityUtils.getUserId());

        return R.ok(menus);
    }
}
