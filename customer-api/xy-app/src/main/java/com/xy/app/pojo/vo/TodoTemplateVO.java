package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 清单模板返回（模板页列表的一行）
 * */
@Data
public class TodoTemplateVO {

    /**
     * 模板 Id
     * */
    @JsonProperty("templateId")
    private Long templateId;

    /**
     * 模板编码，套用时回传给后端
     * */
    @JsonProperty("code")
    private String code;

    /**
     * 模板名称，套用后即为新清单名
     * */
    @JsonProperty("name")
    private String name;

    /**
     * 模板简介
     * */
    @JsonProperty("description")
    private String description;

    /**
     * 分类
     * */
    @JsonProperty("category")
    private String category;

    /**
     * 图标名（对应前端 y-icon 的 name）
     * */
    @JsonProperty("icon")
    private String icon;

    /**
     * 模板预置的任务条数
     * */
    @JsonProperty("itemCount")
    private Integer itemCount;
}
