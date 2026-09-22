package com.xy.common.dto.blog;

import lombok.Data;

@Data
public class ArticleDTO {

    private int pageNum = 1;

    private int pageSize = 10;

    private String title;

    private boolean isRecommend = false;
}
