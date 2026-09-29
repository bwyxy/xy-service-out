package com.xy.admin.controller.blog;

import com.xy.common.core.domain.R;
import com.xy.common.entity.blog.BlogMenuConfig;
import com.xy.system.service.blog.IBlogMenuConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 博客网站菜单配置
 * */
@RequestMapping("/blog/menu/config")
@RestController
@RequiredArgsConstructor
public class BlogMenuConfigController {

    @Autowired
    private IBlogMenuConfigService blogMenuConfigService;

    /**
     * 根据菜单Id 查询配置列表
     * */
    @GetMapping("/{menuId}")
    public R<List<BlogMenuConfig>> list(@PathVariable Long menuId){
        List<BlogMenuConfig> list = blogMenuConfigService.selectBlogMenuConfigByMenuId(menuId);

        return R.ok(list);
    }

    /**
     * 获取配置信息
     * */
    @GetMapping("/info/{id}")
    public R<BlogMenuConfig> info(@PathVariable Long id){
        BlogMenuConfig blogMenuConfig = blogMenuConfigService.selectBlogMenuConfigById(id);

        return R.ok(blogMenuConfig);
    }

    /**
     * 添加配置
     * */
    @PostMapping
    public R<String> addBlogMenuConfig(@RequestBody BlogMenuConfig blogMenuConfig){
        blogMenuConfigService.addBlogMenuConfig(blogMenuConfig);

        return R.ok("添加成功");
    }

    /**
     * 更新配置
     * */
    @PutMapping
    public R<String> updateBlogMenuConfig(@RequestBody BlogMenuConfig blogMenuConfig){
        blogMenuConfigService.editBlogMenuConfig(blogMenuConfig);

        return R.ok("编辑成功");
    }

    /**
     * 删除配置
     * */
    @DeleteMapping("/{menuId}")
    public R<String> deleteBlogMenuConfig(@PathVariable Long menuId){
        blogMenuConfigService.deleteBlogMenuConfig(menuId);

        return R.ok("删除成功");
    }
}
