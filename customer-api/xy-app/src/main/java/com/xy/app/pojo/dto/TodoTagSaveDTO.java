package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 清单新增 / 修改入参
 * */
@Data
public class TodoTagSaveDTO {

    /**
     * 清单名称
     * */
    @JsonProperty("tagName")
    private String tagName;

    /**
     * 图标名（对应前端 y-icon 的 name）
     * */
    @JsonProperty("icon")
    private String icon;

    /**
     * 主题色
     * */
    @JsonProperty("color")
    private String color;

    /**
     * 排序，新增时不传则排到当前末尾
     * */
    @JsonProperty("sortNo")
    private Integer sortNo;
}
