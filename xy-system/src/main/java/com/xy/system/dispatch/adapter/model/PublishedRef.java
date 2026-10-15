package com.xy.system.dispatch.adapter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已发布文章的引用（用于拉取运营数据）
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublishedRef {

    /** 平台侧文章 Id */
    private String platformArticleId;

    /** 平台侧文章访问链接 */
    private String resultUrl;
}
