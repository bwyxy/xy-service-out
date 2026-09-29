package com.xy.blog.controller;

import com.xy.blog.pojo.vo.BulletChatVO;
import com.xy.blog.service.ICategoryService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.vo.blog.ToolCategoryVO;
import com.xy.common.vo.blog.ToolRecommendVO;
import com.xy.system.service.blog.IBlogToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工具
 * */
@RequestMapping("/tool")
@RestController
public class ToolController {

    @Autowired
    private IBlogToolService blogToolService;

    @Autowired
    private ICategoryService categoryService;

    /**
     * 获取推荐工具
     * */
    @Anonymous
    @GetMapping("/recommend")
    public R<?> recommendList() {
        List<ToolRecommendVO> list = blogToolService.selectBlogToolList();

        return R.ok(list);
    }

    /**
     * 查询分类关联工具列表
     * */
    @Anonymous
    @GetMapping("/with/category")
    public R<List<ToolCategoryVO>> withCategory() {
        List<ToolCategoryVO> list = categoryService.listCategoryWithTools();

        return R.ok(list);
    }
}
