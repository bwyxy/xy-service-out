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

@RequestMapping("/blog/menu")
@RestController
@RequiredArgsConstructor
public class BlogMenuController {

    @Autowired
    private IBlogMenuService blogMenuService;

    @GetMapping("/list")
    public R<List<BlogMenu>> menuList() {

        List<BlogMenu> list = blogMenuService.selectMenuTree();

        return R.ok(list);
    }

    @GetMapping("/{menuId}")
    public R<BlogMenu> menuDetail(@PathVariable("menuId") Long menuId) {
        BlogMenu blogMenu = blogMenuService.selectMenuById(menuId);

        return R.ok(blogMenu);
    }

    @PostMapping("/add")
    public R<String> addMenu(@RequestBody AddMenuDTO addMenuDTO) {

        blogMenuService.addMenu(addMenuDTO);

        return R.ok("添加成功");
    }

    @PutMapping("/edit")
    public R<String> editMenu(@RequestBody EditMenuDTO editMenuDTO) {

        blogMenuService.updateMenu(editMenuDTO);

        return R.ok("编辑成功");
    }

    @DeleteMapping("/{id}")
    public R<String> deleteMenu(@PathVariable Long id) {
        blogMenuService.deleteMenu(id);

        return R.ok("删除成功");
    }
}
