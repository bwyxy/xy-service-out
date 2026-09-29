package com.xy.common.vo.blog;

import com.xy.common.entity.blog.BlogTool;
import lombok.Data;

import java.util.List;

@Data
public class ToolCategoryVO {

    private Long categoryId;

    private String categoryName;

    private List<BlogTool> tools;
}
