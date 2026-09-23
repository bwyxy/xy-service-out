package com.xy.common.vo.blog;

import lombok.Data;

@Data
public class ToolRecommendVO {

    private Long id;

    private String name;

    private String url;

    private String icon;

    private String description;

    private String platform;

    private int priceType;
}
