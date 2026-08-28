package com.xy.admin.controller.forum;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.forum.ClearReportDTO;
import com.xy.common.entity.forum.ForumReport;
import com.xy.system.service.forum.IForumReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 论坛举报
 * */
@RestController
@RequestMapping("/admin/forum")
@RequiredArgsConstructor
public class ForumReportController {

    @Autowired
    private IForumReportService forumReportService;

    /**
     * 获取举报列表
     * */
    @PostMapping("/reports")
    public R<PageResult<ForumReport>> reports(@RequestBody ForumReport forumReport){

        return R.ok(forumReportService.getForumReportList(forumReport));
    }

    /**
     * 处理举报
     * */
    @PostMapping("/reports/handle")
    public R<String> handle(@RequestBody ClearReportDTO clearReportDTO){

        forumReportService.clearReport(clearReportDTO);

        return R.ok("操作成功");
    }
}
