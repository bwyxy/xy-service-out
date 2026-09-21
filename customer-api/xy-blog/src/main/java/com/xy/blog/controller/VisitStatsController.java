package com.xy.blog.controller;

import com.xy.blog.pojo.dto.BlogVisitTrackDTO;
import com.xy.blog.pojo.dto.VisitStatsQuery;
import com.xy.blog.service.IVisitStatsService;
import com.xy.blog.service.VisitAntiSpamService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.ip.IpUtils;
import com.xy.common.utils.SecurityUtils;
import com.xy.common.utils.StringUtils;
import com.xy.common.vo.blog.VisitDailyStatsVO;
import com.xy.common.vo.blog.VisitDeviceVO;
import com.xy.common.vo.blog.VisitOverviewVO;
import com.xy.common.vo.blog.VisitTopPageVO;
import com.xy.common.vo.blog.VisitTopSourceVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 网站访问统计接口(前端埋点)
 */
@RestController
@RequestMapping("/web/stats")
@RequiredArgsConstructor
public class VisitStatsController {

    private final IVisitStatsService visitStatsService;
    private final VisitAntiSpamService antiSpamService;

    /**
     * 埋点上报
     */
    @Anonymous
    @PostMapping("/track")
    public R<?> track(@RequestBody @Valid BlogVisitTrackDTO dto, HttpServletRequest request) {
        String ip = IpUtils.getIpAddr(request);
        String visitorId = StringUtils.isBlank(dto.getVisitorId()) ? ip : dto.getVisitorId();

        if (!antiSpamService.isAllowed(ip, visitorId)) {
            return R.ok();
        }

        Long loginUserId = null;
        try {
            loginUserId = SecurityUtils.getUserId();
        } catch (Exception ignored) {
            // 未登录用户允许埋点
        }

        visitStatsService.track(dto, request, loginUserId);
        return R.ok();
    }

    /**
     * 访问概览
     */
    @Anonymous
    @GetMapping("/overview")
    public R<VisitOverviewVO> overview() {
        return R.ok(visitStatsService.getOverview());
    }

    /**
     * 每日访问汇总
     */
    @Anonymous
    @GetMapping("/daily")
    public R<List<VisitDailyStatsVO>> daily(VisitStatsQuery query) {
        return R.ok(visitStatsService.getDailyStats(query));
    }

    /**
     * 热门页面排行
     */
    @Anonymous
    @GetMapping("/top/pages")
    public R<List<VisitTopPageVO>> topPages(VisitStatsQuery query) {
        return R.ok(visitStatsService.getTopPages(query));
    }

    /**
     * 访问来源排行
     */
    @Anonymous
    @GetMapping("/top/sources")
    public R<List<VisitTopSourceVO>> topSources(VisitStatsQuery query) {
        return R.ok(visitStatsService.getTopSources(query));
    }

    /**
     * 设备分布
     */
    @Anonymous
    @GetMapping("/devices")
    public R<List<VisitDeviceVO>> devices(VisitStatsQuery query) {
        return R.ok(visitStatsService.getDeviceDistribution(query));
    }
}
