package com.todo.blog.controller;

import com.todo.blog.pojo.dto.CategoryDTO;
import com.todo.blog.service.ICategoryService;
import com.todo.common.core.domain.R;
import com.todo.common.entity.SysCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/web/category")
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
}
