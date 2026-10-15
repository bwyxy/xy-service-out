package com.xy.common.vo.blog;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分发运营数据聚合统计 VO（按平台维度汇总）
 * */
@Data
public class DispatchStatSummaryVO {

    private String platformCode;

    private String platformName;

    /** 已成功分发的文章数 */
    private Long articleCount;

    /** 浏览量合计 */
    private Long viewCount;

    /** 点赞数合计 */
    private Long likeCount;

    /** 评论数合计 */
    private Long commentCount;

    /** 收藏数合计 */
    private Long collectCount;

    /** 最近一次采集时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastStatTime;
}
