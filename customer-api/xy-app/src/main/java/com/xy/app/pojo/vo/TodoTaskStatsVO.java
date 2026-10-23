package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 我的页统计
 *
 * <p>三个数字都在服务端算好：分页列表接口拿不到「累计完成」和「连续记录」，
 * 前端也不该为了三个数字各发一次请求。</p>
 * */
@Data
public class TodoTaskStatsVO {

    /**
     * 累计完成任务数（含已归档、已清空的）
     * */
    @JsonProperty("doneTotal")
    private Long doneTotal;

    /**
     * 连续记录天数（连续多少天有完成记录，今天或昨天为最后一天）
     * */
    @JsonProperty("streakDays")
    private Integer streakDays;

    /**
     * 我的清单数（未归档的清单）
     * */
    @JsonProperty("tagCount")
    private Long tagCount;
}
