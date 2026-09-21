package com.xy.blog.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 前端埋点上传的访问事件数据
 */
@Data
public class BlogVisitTrackDTO {

    /**
     * 事件类型: page_view / duration / click / submit 等
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 页面路径
     */
    private String path;

    /**
     * 来源页
     */
    private String referrer;

    /**
     * 页面标题
     */
    private String title;

    /**
     * 匿名访客标识
     */
    private String visitorId;

    /**
     * 会话标识
     */
    private String sessionId;

    /**
     * 停留时长(毫秒), 用于 duration 事件
     */
    private Integer duration;

    /**
     * 访问来源标识(如 search / direct / social)
     */
    private String source;

    /**
     * 自定义属性
     */
    private Map<String, Object> props;
}
