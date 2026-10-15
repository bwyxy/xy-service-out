package com.xy.system.dispatch.adapter.model;

import lombok.Data;

/**
 * 平台运营数据采集结果
 * */
@Data
public class StatResult {

    /** 平台侧文章 Id */
    private String platformArticleId;

    /** 浏览量 */
    private int viewCount;

    /** 点赞数 */
    private int likeCount;

    /** 评论数 */
    private int commentCount;

    /** 收藏数 */
    private int collectCount;
}
