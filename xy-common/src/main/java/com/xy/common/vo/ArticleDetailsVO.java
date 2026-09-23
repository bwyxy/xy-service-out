package com.xy.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xy.common.entity.SysTag;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleDetailsVO {
    private Long articleId;

    /**
     * 文章标题
     * */
    private String title;

    /**
     * URL 别名
     * */
    private String slug;

    /**
     * 摘要
     * */
    private String summary;

    /**
     * 正文
     * */
    private String content;

    /**
     * 封面
     * */
    private String coverImage;

    /** 状态
     * 0-草稿 1-已发布 2-私密
     * */
    private String status;

    /**
     * 浏览次数
     * */
    private int viewCount;

    /**
     * 点赞数
     * */
    private int likeCount;

    /**
     * 评论数
     * */
    private int commentCount;

    /**
     * 作者 Id
     * */
    private Long authorId;

    /**
     * 分类 Id
     * */
    private Long categoryId;

    private String categoryName;

    /**
     * 发布时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishedTime;

    /**
     * 标签
     * */
    private List<SysTag> tags;
}
