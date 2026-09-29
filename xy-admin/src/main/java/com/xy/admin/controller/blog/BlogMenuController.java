package com.xy.admin.controller.blog;

import com.xy.common.dto.blog.AddMenuDTO;
import com.xy.common.core.domain.R;
import com.xy.common.dto.blog.EditMenuDTO;
import com.xy.common.entity.blog.BlogMenu;
import com.xy.system.service.blog.IBlogMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 网站菜单
 * */
@RequestMapping("/blog/menu")
@RestController
@RequiredArgsConstructor
public class BlogMenuController {

    @Autowired
    private IBlogMenuService blogMenuService;

    /**
     * 查询列表(树形结构)
     * */
    @GetMapping("/list")
    public R<List<BlogMenu>> menuList() {

        List<BlogMenu> list = blogMenuService.selectMenuTree();

        return R.ok(list);
    }

    /**
     * 查询菜单信息
     * */
    @GetMapping("/{menuId}")
    public R<BlogMenu> menuDetail(@PathVariable Long menuId) {
        BlogMenu blogMenu = blogMenuService.selectMenuById(menuId);

        return R.ok(blogMenu);
    }

    /**
     * 添加
     * */
    @PostMapping("/add")
    public R<String> addMenu(@RequestBody AddMenuDTO addMenuDTO) {

        blogMenuService.addMenu(addMenuDTO);

        return R.ok("添加成功");
    }

    /**
     * 更新
     * */
    @PutMapping("/edit")
    public R<String> editMenu(@RequestBody EditMenuDTO editMenuDTO) {

        blogMenuService.updateMenu(editMenuDTO);

        return R.ok("编辑成功");
    }

    /**
     * 更新
     * */
    @DeleteMapping("/{id}")
    public R<String> deleteMenu(@PathVariable Long id) {
        blogMenuService.deleteMenu(id);

        return R.ok("删除成功");
    }
}
