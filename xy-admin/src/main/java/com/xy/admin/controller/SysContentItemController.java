package com.xy.admin.controller;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysContentItem;
import com.xy.common.enums.ContentItemType;
import com.xy.common.utils.StringUtils;
import com.xy.system.service.ISysContentItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contentItem")
@RequiredArgsConstructor
public class SysContentItemController {

    @Autowired
    private ISysContentItemService sysContentItemService;

    /**
     * 分页查询内容通用项列表
     * */
    @PostMapping("/list")
    public R<?> list(@RequestBody SysContentItem sysContentItem) {
        PageResult<SysContentItem> list = sysContentItemService.selectContentItems(sysContentItem);
        return R.ok(list);
    }

    /**
     * 查询详情
     * */
    @GetMapping("/{id}")
    public R<SysContentItem> selectContentItemById(@PathVariable Long id) {
        SysContentItem sysContentItem = sysContentItemService.selectContentItemById(id);

        return R.ok(sysContentItem);
    }

    /**
     * 新增内容通用项
     * */
    @PostMapping("/add")
    public R<?> add(@RequestBody SysContentItem sysContentItem) {
        if (sysContentItem.getType() == null || StringUtils.isEmpty(sysContentItem.getType())) {
            throw new SecurityException("type 类型不能为空");
        }

        ContentItemType type = ContentItemType.fromCode(sysContentItem.getType());

        sysContentItem.setType(String.valueOf(type));

        sysContentItemService.addContentItem(sysContentItem);

        return R.ok("新增成功");
    }

    /**
     * 更新内容通用项
     * */
    @PutMapping("/update")
    public R<?> update(@RequestBody SysContentItem sysContentItem) {
        sysContentItemService.updateContentItem(sysContentItem);

        return R.ok("更新成功");
    }

    /**
     * 删除内容通用项
     * */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        sysContentItemService.deleteContentItemById(id);

        return R.ok("删除成功");
    }
}
