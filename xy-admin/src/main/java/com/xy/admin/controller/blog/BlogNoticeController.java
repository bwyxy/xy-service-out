package com.xy.admin.controller.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.blog.BlogNotice;
import com.xy.system.service.blog.IBlogNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 网站公告
 * */
@RequestMapping("/blog/notice")
@RestController
public class BlogNoticeController {

    @Autowired
    private IBlogNoticeService blogNoticeService;

    /**
     * 查询公告列表
     * */
    @PostMapping("/list")
    public R<PageResult<BlogNotice>> list(@RequestBody BlogNotice blogNotice) {
        PageResult<BlogNotice> page = blogNoticeService.selectBlogNotice(blogNotice);

        return R.ok(page);
    }

    /**
     * 根据Id 查询公告信息
     * */
    @GetMapping("/{id}")
    public R<BlogNotice> getBlogNotice(@PathVariable Long id) {
        BlogNotice blogNotice = blogNoticeService.selectBlogNoticeById(id);

        return R.ok(blogNotice);
    }

    /**
     * 添加
     * */
    @PostMapping("/add")
    public R<String> addBlogNotice(@RequestBody BlogNotice blogNotice){
        blogNoticeService.addBlogNotice(blogNotice);

        return R.ok("添加成功");
    }

    /**
     * 更新
     * */
    @PutMapping("/update")
    public R<String> updateBlogNotice(@RequestBody BlogNotice blogNotice){
        blogNoticeService.updateBlogNotice(blogNotice);

        return R.ok("更新成功");
    }

    /**
     * 删除
     * */
    @DeleteMapping("/{id}")
    public R<String> deleteBlogNotice(@PathVariable Long id) {
        blogNoticeService.deleteBlogNotice(id);

        return R.ok("删除成功");
    }
}
