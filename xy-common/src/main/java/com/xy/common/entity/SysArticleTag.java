package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 文章标签关联表 sys_article_tag
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_article_tag")
public class SysArticleTag implements Serializable {

    /**
     * 文章 Id
     * */
    private Long articleId;

    /**
     * 标签 Id
     * */
    private Long tagId;
}
