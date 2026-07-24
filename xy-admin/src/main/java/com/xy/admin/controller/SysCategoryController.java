package com.xy.admin.controller;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysCategory;
import com.xy.system.service.ISysCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class SysCategoryController {

    @Autowired
    private ISysCategoryService sysCategoryService;

    /**
     * 分页查询分类列表
     * */
    @PostMapping("/list")
    public R<?> list(@RequestBody SysCategory sysCategory) {

        PageResult<SysCategory> pageSysCategory = sysCategoryService.selectSysCategoryList(sysCategory);

        return R.ok(pageSysCategory);
    }

    /**
     * 根据分类 Id查询分类信息
     * */
    @GetMapping("/{id}")
    public R<SysCategory> getSysCategoryById(@PathVariable Long id) {
        SysCategory sysCategory = sysCategoryService.selectSysCategoryById(id);

        return R.ok(sysCategory);
    }

    /**
     * 新增分类
     * */
    @PostMapping("/add")
    public R<?> addSysCategory(@RequestBody SysCategory sysCategory) {
        sysCategoryService.insertSysCategory(sysCategory);

        return R.ok("新增成功");
    }

    /**
     * 更新分类信息
     * */
    @PutMapping("/update")
    public R<?> updateSysCategory(@RequestBody SysCategory sysCategory) {
        sysCategoryService.updateSysCategory(sysCategory);

        return R.ok("更新成功");
    }

    /**
     * 删除分类
     * */
    @DeleteMapping("/{id}")
    public R<?> deleteSysCategoryById(@PathVariable Long id) {
        sysCategoryService.deleteSysCategoryById(id);

        return R.ok("删除成功");
    }
}
