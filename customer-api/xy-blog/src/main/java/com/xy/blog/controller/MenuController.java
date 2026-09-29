package com.xy.blog.controller;

import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.entity.blog.BlogMenu;
import com.xy.system.service.blog.IBlogMenuConfigService;
import com.xy.system.service.blog.IBlogMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 查询菜单配置
 * */
@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private IBlogMenuService blogMenuService;

    @Autowired
    private IBlogMenuConfigService blogMenuConfigService;

    /**
     * 获取菜单树
     * */
    @Anonymous
    @GetMapping("/tree")
    public R<List<BlogMenu>> menuTree(){
        List<BlogMenu> list = blogMenuService.selectMenuTree();

        return R.ok(list);
    }

    /**
     * 查询菜单配置信息
     * */
    @Anonymous
    @GetMapping("/{menuId}/config")
    public R<Map<String, Object>> config(@PathVariable Long menuId){

        Map<String, Object> configMap = blogMenuConfigService.getBlogMenuConfigByMenuId(menuId);

        return R.ok(configMap);
    }
}
