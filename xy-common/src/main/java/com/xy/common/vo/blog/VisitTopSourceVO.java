package com.xy.common.vo.blog;

import lombok.Data;

/**
 * 访问统计-来源排行
 */
@Data
public class VisitTopSourceVO {

    /**
     * 来源地址
     */
    private String referrer;

    /**
     * 浏览量
     */
    private Long pv;

    /**
     * 独立访客
     */
    private Long uv;
}
