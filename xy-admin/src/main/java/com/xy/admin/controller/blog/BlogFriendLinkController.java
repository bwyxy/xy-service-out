package com.xy.admin.controller.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.blog.BlogFriendLink;
import com.xy.system.service.blog.IBlogFriendLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/blog/friendLink")
@RestController
public class BlogFriendLinkController {

    @Autowired
    private IBlogFriendLinkService blogFriendLinkService;

    @PostMapping("/list")
    public R<PageResult<BlogFriendLink>> list(@RequestBody BlogFriendLink blogFriendLink){
        PageResult<BlogFriendLink> page = blogFriendLinkService.selectBlogFriendListByPage(blogFriendLink);

        return R.ok(page);
    }

    @GetMapping("/{id}")
    public R<BlogFriendLink> getById(@PathVariable Long id){
        BlogFriendLink blogFriendLink = blogFriendLinkService.selectBlogFriendLinkById(id);

        return R.ok(blogFriendLink);
    }

    @PostMapping("/add")
    public R<String> add(@RequestBody BlogFriendLink blogFriendLink){
        blogFriendLinkService.insertBlogFriendLink(blogFriendLink);

        return R.ok("添加友链");
    }

    @PutMapping("/update")
    public R<String> update(@RequestBody BlogFriendLink blogFriendLink){
        blogFriendLinkService.updateBlogFriendLink(blogFriendLink);

        return R.ok("更新友链");
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id){
        blogFriendLinkService.deleteBlogFriendLinkById(id);

        return R.ok("删除成功");
    }
}
