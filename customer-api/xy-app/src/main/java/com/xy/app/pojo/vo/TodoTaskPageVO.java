package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 任务分页列表
 *
 * <p>除了当前页数据，一并带上清单维度的三个计数：列表分页后前端无法再靠本地数据统计
 * 「全部/待完成/已完成」的条数，所以由后端一次 group by 算好返回。</p>
 * */
@Data
public class TodoTaskPageVO {

    /**
     * 当前页任务
     * */
    @JsonProperty("list")
    private List<TodoTaskVO> list;

    /**
     * 当前筛选状态下的总条数
     * */
    @JsonProperty("total")
    private Long total;

    /**
     * 当前页码
     * */
    @JsonProperty("pageNum")
    private Long pageNum;

    /**
     * 每页条数
     * */
    @JsonProperty("pageSize")
    private Long pageSize;

    /**
     * 是否还有下一页
     * */
    @JsonProperty("hasMore")
    private Boolean hasMore;

    /**
     * 该清单（或全部清单）下未归档任务总数
     * */
    @JsonProperty("allCount")
    private Long allCount;

    /**
     * 未完成条数
     * */
    @JsonProperty("pendingCount")
    private Long pendingCount;

    /**
     * 已完成条数
     * */
    @JsonProperty("doneCount")
    private Long doneCount;
}
