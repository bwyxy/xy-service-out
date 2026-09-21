package com.xy.system.mapper.blog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.blog.BlogEventLog;
import com.xy.common.vo.blog.VisitDailyStatsVO;
import com.xy.common.vo.blog.VisitDeviceVO;
import com.xy.common.vo.blog.VisitTopPageVO;
import com.xy.common.vo.blog.VisitTopSourceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客访问日志 Mapper
 */
@Mapper
public interface BlogEventLogMapper extends BaseMapper<BlogEventLog> {

    /**
     * 查询每日访问汇总
     */
    List<VisitDailyStatsVO> selectDailyStats(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 查询热门页面
     */
    List<VisitTopPageVO> selectTopPages(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("limit") int limit);

    /**
     * 查询来源排行
     */
    List<VisitTopSourceVO> selectTopSources(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            @Param("limit") int limit);

    /**
     * 查询设备分布
     */
    List<VisitDeviceVO> selectDeviceDistribution(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 查询累计 PV/UV(截止到指定时间)
     */
    VisitDailyStatsVO selectTotalPvUv(@Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定日期的汇总
     */
    VisitDailyStatsVO selectDayStats(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);
}
