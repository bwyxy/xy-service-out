package com.xy.admin.controller;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysTag;
import com.xy.system.service.ISysTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tag")
@RequiredArgsConstructor
public class SysTagController {

    @Autowired
    private ISysTagService sysTagService;

    /**
     * 分页查询标签列表
     * */
    @PostMapping("/list")
    public R<?> list(@RequestBody SysTag sysTag) {
        PageResult<SysTag> sysTags = sysTagService.selectSysTagList(sysTag);

        return R.ok(sysTags);
    }

    /**
     * 根据标签 Id查询标签信息
     * */
    @GetMapping("/{id}")
    public R<SysTag> getSysTagById(@PathVariable Long id) {
        SysTag sysTag = sysTagService.selectSysTagById(id);

        return R.ok(sysTag);
    }

    /**
     * 添加标签
     * */
    @PostMapping("/add")
    public R<?> addSysTag(@RequestBody SysTag sysTag) {
        sysTagService.insertSysTag(sysTag);

        return R.ok("添加成功");
    }

    /**
     * 更新标签
     * */
    @PutMapping("/update")
    public R<?> updateSysTag(@RequestBody SysTag sysTag) {
        sysTagService.updateSysTag(sysTag);

        return R.ok("更新成功");
    }

    /**
     * 删除标签
     * */
    @DeleteMapping("/{id}")
    public R<?> deleteSysTagById(@PathVariable Long id) {
        sysTagService.deleteSysTagById(id);

        return R.ok("删除成功");
    }
}
