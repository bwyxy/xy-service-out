package com.xy.common.vo.blog;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVO {

    private Long articleId;

    private String title;

    private String slug;

    private String coverImage;

    private Integer viewCount;

    private Integer commentCount;

    private Integer likeCount;

    private LocalDateTime publishedTime;

    private String tagNames;
}
