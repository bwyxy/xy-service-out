package com.xy.common.vo.blog;

import lombok.Data;

@Data
public class NoticeVO {

    private Long id;

    private String title;

    private String content;

    private Integer type;

    private String linkUrl;
}
