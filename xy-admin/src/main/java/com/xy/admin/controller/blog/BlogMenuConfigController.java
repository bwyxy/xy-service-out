package com.xy.admin.controller.blog;

import com.xy.common.core.domain.R;
import com.xy.common.entity.blog.BlogMenuConfig;
import com.xy.system.service.blog.IBlogMenuConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/blog/menu/config")
@RestController
@RequiredArgsConstructor
public class BlogMenuConfigController {

    @Autowired
    private IBlogMenuConfigService blogMenuConfigService;

    @GetMapping("/{menuId}")
    public R<List<BlogMenuConfig>> list(@PathVariable Long menuId){
        List<BlogMenuConfig> list = blogMenuConfigService.selectBlogMenuConfigByMenuId(menuId);

        return R.ok(list);
    }

    @GetMapping("/info/{id}")
    public R<BlogMenuConfig> info(@PathVariable Long id){
        BlogMenuConfig blogMenuConfig = blogMenuConfigService.selectBlogMenuConfigById(id);

        return R.ok(blogMenuConfig);
    }

    @PostMapping
    public R<String> addBlogMenuConfig(@RequestBody BlogMenuConfig blogMenuConfig){
        blogMenuConfigService.addBlogMenuConfig(blogMenuConfig);

        return R.ok("添加成功");
    }

    @PutMapping
    public R<String> updateBlogMenuConfig(@RequestBody BlogMenuConfig blogMenuConfig){
        blogMenuConfigService.editBlogMenuConfig(blogMenuConfig);

        return R.ok("编辑成功");
    }

    @DeleteMapping("/{menuId}")
    public R<String> deleteBlogMenuConfig(@PathVariable Long menuId){
        blogMenuConfigService.deleteBlogMenuConfig(menuId);

        return R.ok("删除成功");
    }
}
