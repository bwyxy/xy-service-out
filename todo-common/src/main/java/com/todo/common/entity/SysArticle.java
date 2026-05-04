package com.todo.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.todo.common.core.domain.BaseEntity;
import jakarta.persistence.Convert;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章表 sys_article
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_article")
public class SysArticle extends BaseEntity implements Serializable {

    /**
     * 文章 Id
     * */
    @TableId(value = "article_id", type = IdType.AUTO)
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

    /**
     * 发布时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishedTime;
}
