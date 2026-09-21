package com.xy.common.vo.blog;

import lombok.Data;
import java.util.List;

@Data
public class CategoryVO {

    private Long categoryId;

    private String name;

    private List<ArticleVO> articles;
}
