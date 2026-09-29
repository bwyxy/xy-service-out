package com.xy.system.service.blog;

import com.xy.common.dto.blog.BlogVisitTrackDTO;
import com.xy.common.dto.blog.VisitStatsQuery;
import com.xy.common.vo.blog.VisitDailyStatsVO;
import com.xy.common.vo.blog.VisitDeviceVO;
import com.xy.common.vo.blog.VisitOverviewVO;
import com.xy.common.vo.blog.VisitTopPageVO;
import com.xy.common.vo.blog.VisitTopSourceVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 网站访问统计服务
 */
public interface IVisitStatsService {

    /**
     * 接收前端埋点数据
     */
    void track(BlogVisitTrackDTO dto, HttpServletRequest request, Long loginUserId);

    /**
     * 查询访问概览
     */
    VisitOverviewVO getOverview();

    /**
     * 查询每日访问汇总
     */
    List<VisitDailyStatsVO> getDailyStats(VisitStatsQuery query);

    /**
     * 查询热门页面
     */
    List<VisitTopPageVO> getTopPages(VisitStatsQuery query);

    /**
     * 查询来源排行
     */
    List<VisitTopSourceVO> getTopSources(VisitStatsQuery query);

    /**
     * 查询设备分布
     */
    List<VisitDeviceVO> getDeviceDistribution(VisitStatsQuery query);
}
