package com.xy.system.service;

import java.util.List;

public interface ISysArticleTagService {

    /**
     * 添加时文章和标签的关联
     * */
    public void addArticleTags(Long articleId, List<Long> tagIds);

    /**
     * 更新添加标签和文章的关联
     * */
    public void updateArticleTags(Long articleId, List<Long> tagIds);
}
