package com.todo.admin.controller;

import com.todo.common.core.domain.R;
import com.todo.common.entity.SysMenu;
import com.todo.common.utils.SecurityUtils;
import com.todo.system.service.ISysMenuService;
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
