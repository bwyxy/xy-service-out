package com.xy.blog.controller;

import com.xy.blog.pojo.dto.CategoryDTO;
import com.xy.blog.service.ICategoryService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysCategory;
import com.xy.common.vo.blog.CategoryVO;
import com.xy.system.service.ISysCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类
 * */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private ICategoryService categoryService;

    /**
     * 获取分类列表
     * */
    @PostMapping("/list")
    public R<?> list(@RequestBody CategoryDTO categoryDTO) {

        String keyword = categoryDTO.getKeyword();

        List<SysCategory> categories = categoryService.getCategoryAll(keyword);

        return R.ok(categories);
    }

    /**
     * 查询分类关联文章列表
     * */
    @Anonymous
    @PostMapping("/with/articles")
    public R<?> withArticles() {
        List<CategoryVO> list = categoryService.listCategoryWithArticles();

        return R.ok(list);
    }

    /**
     * 获取相册分类
     * */
    @Anonymous
    @GetMapping("/album")
    public R<?> album() {
        List<SysCategory> list = categoryService.listAlbumCategory();
        return R.ok(list);
    }

    /**
     * 获取工具分类
     * */
    @Anonymous
    @GetMapping("/tool")
    public R<?> tool() {
        List<SysCategory> list = categoryService.listToolCategory();

        return R.ok(list);
    }
}
