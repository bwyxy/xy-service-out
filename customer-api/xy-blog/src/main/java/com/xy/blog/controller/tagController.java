package com.xy.blog.controller;

import com.xy.blog.pojo.vo.TagListVO;
import com.xy.blog.service.ITagService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysTag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tag")
@RequiredArgsConstructor
public class tagController {

    @Autowired
    private ITagService tagService;

    /**
     * 获取标签列表
     * */
    @Anonymous
    @PostMapping("/list")
    public R<?> getTagList() {
        List<TagListVO> list = tagService.getSysTagList();
        return R.ok(list);
    }

    /**
     * 获取技能级别标签列表
     * */
    @PostMapping("/level/list")
    public R<List<SysTag>> getTagListByLevel() {
        List<SysTag> list = tagService.getSysTagByLevel();

        return R.ok(list);
    }
}
