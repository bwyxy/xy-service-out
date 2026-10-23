package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 四象限 · 单个象限
 *
 * <p>计数查询（group by urgency_level）复用本类承接结果，只回填 urgencyLevel / total 两个字段。</p>
 * */
@Data
public class TodoQuadrantGroupVO {

    /**
     * 紧急程度 1-不重要不紧急 2-紧急不重要 3-重要不紧急 4-重要且紧急
     * */
    @JsonProperty("urgencyLevel")
    private Integer urgencyLevel;

    /**
     * 该象限待完成任务数（未完成、未归档、未删除）
     * */
    @JsonProperty("total")
    private Long total;

    /**
     * 该象限的待完成任务预览（按到期时间正序，没设日期的排最后）
     *
     * <p>格子里只放得下前几条，看全量走 POST /task/list 的 urgencyLevel 过滤。</p>
     * */
    @JsonProperty("tasks")
    private List<TodoTaskVO> tasks;
}
