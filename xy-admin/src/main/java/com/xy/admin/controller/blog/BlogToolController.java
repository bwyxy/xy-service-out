package com.xy.admin.controller.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.blog.BlogTool;
import com.xy.system.service.blog.IBlogToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 工具
 * */
@RequestMapping("/blog/tool")
@RestController
public class BlogToolController {

    @Autowired
    private IBlogToolService blogToolService;

    /**
     * 工具列表
     * */
    @PostMapping("/list")
    public R<PageResult<BlogTool>> list(@RequestBody BlogTool blogTool){

        PageResult<BlogTool> page = blogToolService.getBlogToolList(blogTool);

        return R.ok(page);
    }

    /**
     * 根据Id 查询工具信息
     * */
    @GetMapping("/{id}")
    public R<BlogTool> getBlogToolById(@PathVariable Long id){

        BlogTool blogTool = blogToolService.getBlogToolById(id);

        return R.ok(blogTool);
    }

    /**
     * 添加
     * */
    @PostMapping("/add")
    public R<String> addBlogTool(@RequestBody BlogTool blogTool){
        blogToolService.addBlogTool(blogTool);

        return R.ok("添加成功");
    }

    /**
     * 更新
     * */
    @PutMapping("/update")
    public R<String> updateBlogTool(@RequestBody BlogTool blogTool){
        blogToolService.updateBlogTool(blogTool);

        return R.ok("更新成功");
    }

    /**
     * 删除
     * */
    @DeleteMapping("/{id}")
    public R<String> deleteBlogTool(@PathVariable Long id){
        blogToolService.deleteBlogTool(id);

        return R.ok("删除成功");
    }
}
