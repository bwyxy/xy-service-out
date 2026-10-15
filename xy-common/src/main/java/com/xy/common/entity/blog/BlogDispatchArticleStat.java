package com.xy.common.entity.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 分发文章运营数据表 blog_dispatch_article_stat
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_dispatch_article_stat")
public class BlogDispatchArticleStat extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 分发任务 Id */
    private Long taskId;

    /** 文章 Id */
    private Long articleId;

    /** 平台编码 */
    private String platformCode;

    /** 平台侧文章 Id */
    private String platformArticleId;

    /** 平台侧文章访问链接 */
    private String resultUrl;

    /** 浏览量 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 收藏数 */
    private Integer collectCount;

    /** 最近一次采集时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime statTime;

    /** 文章标题（联表查询填充，不落库） */
    @TableField(exist = false)
    private String articleTitle;
}
