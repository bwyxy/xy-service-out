package com.xy.admin.controller.blog;

import com.xy.common.core.domain.R;
import com.xy.common.dto.blog.VisitStatsQuery;
import com.xy.common.vo.blog.VisitDailyStatsVO;
import com.xy.common.vo.blog.VisitDeviceVO;
import com.xy.common.vo.blog.VisitOverviewVO;
import com.xy.common.vo.blog.VisitTopPageVO;
import com.xy.common.vo.blog.VisitTopSourceVO;
import com.xy.system.service.blog.IVisitStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 博客访问统计管理接口
 */
@RestController
@RequestMapping("/blog/stats")
@RequiredArgsConstructor
public class BlogVisitStatsController {

    private final IVisitStatsService visitStatsService;

    /**
     * 访问概览
     */
    @GetMapping("/overview")
    public R<VisitOverviewVO> overview() {
        return R.ok(visitStatsService.getOverview());
    }

    /**
     * 每日访问汇总
     */
    @GetMapping("/daily")
    public R<List<VisitDailyStatsVO>> daily(VisitStatsQuery query) {
        return R.ok(visitStatsService.getDailyStats(query));
    }

    /**
     * 热门页面排行
     */
    @GetMapping("/top/pages")
    public R<List<VisitTopPageVO>> topPages(VisitStatsQuery query) {
        return R.ok(visitStatsService.getTopPages(query));
    }

    /**
     * 访问来源排行
     */
    @GetMapping("/top/sources")
    public R<List<VisitTopSourceVO>> topSources(VisitStatsQuery query) {
        return R.ok(visitStatsService.getTopSources(query));
    }

    /**
     * 设备分布
     */
    @GetMapping("/devices")
    public R<List<VisitDeviceVO>> devices(VisitStatsQuery query) {
        return R.ok(visitStatsService.getDeviceDistribution(query));
    }
}
