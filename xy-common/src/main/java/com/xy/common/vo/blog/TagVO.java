package com.xy.common.vo.blog;

import lombok.Data;

@Data
public class TagVO {

    private Long tagId;

    private String tagName;

    private String color;
    
    private String slug;

    private String level;
}
