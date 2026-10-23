package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 任务列表查询入参
 *
 * <p>注意：不接受 user_id，所属用户一律由 token 解析（TodoUserDetails.getCurrentUId）。</p>
 * */
@Data
public class TodoTaskQueryDTO {

    /**
     * 清单 Id，不传表示不限清单
     * */
    @JsonProperty("tagId")
    private Long tagId;

    /**
     * 状态筛选 all-全部（默认） pending-待完成 done-已完成
     * */
    @JsonProperty("status")
    private String status;

    /**
     * 搜索关键词，按任务标题与备注模糊匹配；不传表示不搜索
     * */
    @JsonProperty("keyword")
    private String keyword;

    /**
     * 到期日筛选，格式 yyyy-MM-dd，取当天 00:00:00 ~ 次日 00:00:00 的区间；不传表示不限日期
     * */
    @JsonProperty("date")
    private String date;

    /**
     * 紧急程度筛选 1-不重要不紧急 2-紧急不重要 3-重要不紧急 4-重要且紧急；不传表示不限象限
     * */
    @JsonProperty("urgencyLevel")
    private Integer urgencyLevel;

    /**
     * 归档筛选 0-不归档（默认，任务列表与首页用） 1-已归档（已完成与归档页用）
     * */
    @JsonProperty("archived")
    private Integer archived;

    /**
     * 页码，从 1 开始
     * */
    @JsonProperty("pageNum")
    private Integer pageNum;

    /**
     * 每页条数
     * */
    @JsonProperty("pageSize")
    private Integer pageSize;
}
