package com.xy.admin.controller.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.blog.BlogPhoto;
import com.xy.system.service.blog.IBlogPhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 照片
 * */
@RequestMapping("/blog/photo")
@RestController
public class BlogPhotoController {

    @Autowired
    private IBlogPhotoService blogPhotoService;

    /**
     * 查询照片列表
     * */
    @PostMapping("/list")
    public R<PageResult<BlogPhoto>> list(@RequestBody BlogPhoto blogPhoto) {

        PageResult<BlogPhoto> page = blogPhotoService.getBlogPhotoByPage(blogPhoto);

        return R.ok(page);
    }

    /**
     * 查询照片信息
     * */
    @GetMapping("/{id}")
    public R<BlogPhoto> getBlogPhoto(@PathVariable Long id) {
        BlogPhoto blogPhoto = blogPhotoService.getBlogPhotoById(id);

        return R.ok(blogPhoto);
    }

    /**
     * 添加
     * */
    @PostMapping("/add")
    public R<String> addBlogPhoto(@RequestBody BlogPhoto blogPhoto) {
        blogPhotoService.addBlogPhoto(blogPhoto);

        return R.ok("添加成功");
    }

    /**
     * 更新
     * */
    @PutMapping("/edit")
    public R<String> editBlogPhoto(@RequestBody BlogPhoto blogPhoto) {
        blogPhotoService.updateBlogPhoto(blogPhoto);

        return R.ok("更新成功");
    }

    /**
     * 删除
     * */
    @DeleteMapping("/{id}")
    public R<String> deleteBlogPhoto(@PathVariable Long id) {
        blogPhotoService.deleteBlogPhoto(id);

        return R.ok("删除成功");
    }
}
