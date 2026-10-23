package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 单个清单的分布（统计页「清单分布」的一行）
 * */
@Data
public class TodoTagStatVO {

    /**
     * 清单 Id，null 表示未归类
     * */
    @JsonProperty("tagId")
    private Long tagId;

    /**
     * 清单名，未归类为「未归类」
     * */
    @JsonProperty("tagName")
    private String tagName;

    /**
     * 清单主题色，可能为空串（前端补默认色）
     * */
    @JsonProperty("color")
    private String color;

    /**
     * 该清单下的任务数
     * */
    @JsonProperty("total")
    private Long total;
}
