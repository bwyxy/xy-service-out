package com.xy.app.controller;

import com.xy.app.pojo.dto.TodoTagSaveDTO;
import com.xy.app.pojo.vo.TodoTagVO;
import com.xy.app.service.ITagService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端待办清单
 * */
@RestController
@RequestMapping("/tag")
@RequiredArgsConstructor
public class TagController {

    private final ITagService tagService;

    /**
     * 清单列表（含各清单待完成数）
     * */
    @GetMapping("/list")
    public R<List<TodoTagVO>> list() {
        return R.ok(tagService.listTags());
    }

    /**
     * 新增清单
     * */
    @PostMapping("/create")
    public R<TodoTagVO> create(@RequestBody TodoTagSaveDTO dto) {
        return R.ok(tagService.createTag(dto));
    }

    /**
     * 修改清单
     * */
    @PutMapping("/{tagId}")
    public R<Void> update(@PathVariable Long tagId, @RequestBody TodoTagSaveDTO dto) {
        tagService.updateTag(tagId, dto);

        return R.ok();
    }

    /**
     * 删除清单（清单下的任务改为未归类）
     * */
    @DeleteMapping("/{tagId}")
    public R<Void> remove(@PathVariable Long tagId) {
        tagService.removeTag(tagId);

        return R.ok();
    }
}
