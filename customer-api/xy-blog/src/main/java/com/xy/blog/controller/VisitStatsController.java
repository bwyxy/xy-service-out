package com.xy.blog.controller;

import com.xy.blog.service.VisitAntiSpamService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.dto.blog.BlogVisitTrackDTO;
import com.xy.common.ip.IpUtils;
import com.xy.common.utils.SecurityUtils;
import com.xy.common.utils.StringUtils;
import com.xy.system.service.blog.IVisitStatsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网站访问统计埋点接口
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
}
