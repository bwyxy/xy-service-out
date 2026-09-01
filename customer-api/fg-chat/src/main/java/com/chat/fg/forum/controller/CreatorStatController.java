package com.chat.fg.forum.controller;

import com.chat.fg.pojo.vo.CreatorStatsVO;
import com.chat.fg.service.forum.ICreatorStatService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/forum/creator")
@RequiredArgsConstructor
public class CreatorStatController {

    @Autowired
    private ICreatorStatService creatorStatService;

    /**
     * 获取创作中心统计
     * */
    @GetMapping("/stat")
    public R<CreatorStatsVO> getCreatorStats() {

        return R.ok(creatorStatService.getCreatorStats());
    }
}
