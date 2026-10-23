package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 清单返回
 * */
@Data
public class TodoTagVO {

    /**
     * 清单 Id
     * */
    private Long tagId;

    /**
     * 清单名称
     * */
    private String tagName;

    /**
     * 图标名（对应前端 y-icon 的 name）
     * */
    private String icon;

    /**
     * 主题色
     * */
    private String color;

    /**
     * 排序
     * */
    private Integer sortNo;

    /**
     * 是否默认清单 0-否 1-是
     * */
    private Integer isDefault;

    /**
     * 该清单下待完成的任务数
     * */
    private Long pendingCount;

    /**
     * 创建时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
