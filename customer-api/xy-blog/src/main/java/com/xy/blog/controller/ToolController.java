package com.xy.blog.controller;

import com.xy.blog.pojo.vo.BulletChatVO;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.vo.blog.ToolRecommendVO;
import com.xy.system.service.blog.IBlogToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/tool")
@RestController
public class ToolController {

    @Autowired
    private IBlogToolService blogToolService;

    @Anonymous
    @GetMapping("/recommend")
    public R<?> recommendList() {
        List<ToolRecommendVO> list = blogToolService.selectBlogToolList();

        return R.ok(list);
    }
}
