package com.xy.admin.controller;

import com.xy.common.core.domain.R;
import com.xy.common.entity.SysMenu;
import com.xy.common.utils.SecurityUtils;
import com.xy.system.service.ISysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    /** 根据菜单ID查询
     * */
    @GetMapping("/{menuId}")
    public R<?> getInfo(@PathVariable Long menuId) {
        return R.ok(sysMenuService.selectMenuById(menuId));
    }

    /** 新增菜单
     * */
    @PostMapping
    public R<?> add(@RequestBody SysMenu menu) {
        if (!sysMenuService.checkMenuNameUnique(menu)) {
            return R.error(500, "菜单名称已存在");
        }
        return R.ok(sysMenuService.insertMenu(menu));
    }

    /** 修改菜单
     * */
    @PutMapping
    public R<?> edit(@RequestBody SysMenu menu) {
        if (!sysMenuService.checkMenuNameUnique(menu)) {
            return R.error(500, "菜单名称已存在");
        }
        return R.ok(sysMenuService.updateMenu(menu));
    }

    /** 删除菜单
     * */
    @DeleteMapping("/{menuId}")
    public R<?> remove(@PathVariable Long menuId) {
        sysMenuService.deleteMenuById(menuId);
        return R.ok();
    }
}
