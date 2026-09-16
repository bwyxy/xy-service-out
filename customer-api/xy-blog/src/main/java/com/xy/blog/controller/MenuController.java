package com.xy.blog.controller;

import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.entity.blog.BlogMenu;
import com.xy.system.service.blog.IBlogMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 查询菜单配置
 * */
@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private IBlogMenuService blogMenuService;

    @Anonymous
    @GetMapping("/tree")
    public R<List<BlogMenu>> menuTree(){
        List<BlogMenu> list = blogMenuService.selectMenuTree();

        return R.ok(list);
    }
}
