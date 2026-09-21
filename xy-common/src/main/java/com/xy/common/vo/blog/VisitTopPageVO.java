package com.xy.common.vo.blog;

import lombok.Data;

/**
 * 访问统计-热门页面
 */
@Data
public class VisitTopPageVO {

    /**
     * 页面路径
     */
    private String path;

    /**
     * 页面标题
     */
    private String title;

    /**
     * 浏览量
     */
    private Long pv;

    /**
     * 独立访客
     */
    private Long uv;
}
