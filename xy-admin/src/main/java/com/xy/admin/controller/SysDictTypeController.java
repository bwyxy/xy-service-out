package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysDictType;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.ISysDictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 字典类型管理
 * */
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SysDictTypeController {

    private final ISysDictTypeService dictTypeService;

    /**
     * 查询字典类型列表
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:list')")
    @PostMapping("/list")
    public R<PageResult<SysDictType>> list(@RequestBody SysDictType dictType) {
        return R.ok(dictTypeService.selectDictTypeList(dictType));
    }

    /**
     * 根据字典主键查询详情
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:query')")
    @GetMapping("/{dictId}")
    public R<SysDictType> getInfo(@PathVariable Long dictId) {
        return R.ok(dictTypeService.selectDictTypeById(dictId));
    }

    /**
     * 新增字典类型
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:add')")
    @Log(title = "字典类型", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@RequestBody SysDictType dictType) {
        return R.ok(dictTypeService.insertDictType(dictType));
    }

    /**
     * 修改字典类型
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:edit')")
    @Log(title = "字典类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody SysDictType dictType) {
        dictTypeService.updateDictType(dictType);
        return R.ok();
    }

    /**
     * 删除字典类型
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:remove')")
    @Log(title = "字典类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dictId}")
    public R<Void> remove(@PathVariable Long dictId) {
        dictTypeService.deleteDictTypeById(dictId);
        return R.ok();
    }
}
