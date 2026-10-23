package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 套用模板结果返回
 * */
@Data
public class TodoTemplateApplyVO {

    /**
     * 新建清单的 Id
     * */
    @JsonProperty("tagId")
    private Long tagId;

    /**
     * 新建清单的名称（重名时后端加了序号，前端提示要用这个而不是模板名）
     * */
    @JsonProperty("tagName")
    private String tagName;

    /**
     * 实际新建的任务条数
     * */
    @JsonProperty("taskCount")
    private Integer taskCount;
}
