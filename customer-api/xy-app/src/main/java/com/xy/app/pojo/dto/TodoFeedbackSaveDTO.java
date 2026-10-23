package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 提交意见反馈的入参
 *
 * <p>注意：不接受 user_id，提交人一律由 token 解析（TodoUserDetails.getCurrentUId）。</p>
 * */
@Data
public class TodoFeedbackSaveDTO {

    /**
     * 反馈内容（必填）
     * */
    @JsonProperty("content")
    private String content;

    /**
     * 联系方式（选填，用户愿意被回复时才填）
     * */
    @JsonProperty("contact")
    private String contact;

    /**
     * 配图 URL 列表（选填），落库时拼成逗号分隔
     * */
    @JsonProperty("images")
    private List<String> images;
}
