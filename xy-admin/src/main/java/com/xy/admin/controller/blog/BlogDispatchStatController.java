package com.xy.admin.controller.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.blog.DispatchStatQueryDTO;
import com.xy.common.entity.blog.BlogDispatchArticleStat;
import com.xy.common.vo.blog.DispatchStatSummaryVO;
import com.xy.system.service.blog.IBlogDispatchStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分发数据统计
 * */
@RestController
@RequestMapping("/blog/dispatch/stat")
@RequiredArgsConstructor
public class BlogDispatchStatController {

    private final IBlogDispatchStatService statService;

    /**
     * 分页查询分发运营数据
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:stat:view')")
    @PostMapping("/list")
    public R<PageResult<BlogDispatchArticleStat>> list(@RequestBody DispatchStatQueryDTO query) {
        return R.ok(statService.selectStatPage(query));
    }

    /**
     * 按平台聚合统计运营数据
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:stat:view')")
    @GetMapping("/summary")
    public R<List<DispatchStatSummaryVO>> summary(@RequestParam(required = false) String platformCode) {
        return R.ok(statService.selectPlatformSummary(platformCode));
    }

    /**
     * 手动触发运营数据同步
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:stat:sync')")
    @PostMapping("/sync")
    public R<String> sync() {
        int count = statService.refreshStats();
        return R.ok("同步完成，更新 " + count + " 条记录");
    }
}
