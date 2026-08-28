package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysDictData;

import java.util.List;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.ISysDictDataService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 字典数据管理
 * */
@RestController
@RequestMapping("/system/dict/data")
@RequiredArgsConstructor
public class SysDictDataController {

    private final ISysDictDataService dictDataService;

    /**
     * 查询字典数据列表
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:data:list')")
    @PostMapping("/list")
    public R<PageResult<SysDictData>> list(@RequestBody SysDictData dictData) {
        return R.ok(dictDataService.selectDictDataList(dictData));
    }

    /**
     * 根据字典类型查询字典数据
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:data:list')")
    @GetMapping("/type/{dictType}")
    public R<List<SysDictData>> listByType(@PathVariable String dictType) {
        return R.ok(dictDataService.selectDictDataByType(dictType));
    }

    /**
     * 根据字典编码查询详情
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:data:query')")
    @GetMapping("/{dictCode}")
    public R<SysDictData> getInfo(@PathVariable Long dictCode) {
        return R.ok(dictDataService.selectDictDataById(dictCode));
    }

    /**
     * 新增字典数据
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:data:add')")
    @Log(title = "字典数据", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@RequestBody SysDictData dictData) {
        return R.ok(dictDataService.insertDictData(dictData));
    }

    /**
     * 修改字典数据
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:data:edit')")
    @Log(title = "字典数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody SysDictData dictData) {
        dictDataService.updateDictData(dictData);
        return R.ok();
    }

    /**
     * 删除字典数据
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:data:remove')")
    @Log(title = "字典数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dictCode}")
    public R<Void> remove(@PathVariable Long dictCode) {
        dictDataService.deleteDictDataById(dictCode);
        return R.ok();
    }

    /**
     * 导出字典数据
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:data:export')")
    @Log(title = "字典数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, @RequestBody SysDictData dictData) {
        dictDataService.exportDictData(dictData, response);
    }

    /**
     * 导入字典数据
     * */
    @PreAuthorize("@auth.hasPermi('system:dict:data:import')")
    @Log(title = "字典数据", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public R<String> importData(@RequestParam("file") MultipartFile file) {
        return R.ok(dictDataService.importDictData(file));
    }
}
