package com.todo.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.todo.common.entity.SysArticleTag;
import com.todo.system.mapper.SysArticleTagMapper;
import com.todo.system.service.ISysArticleTagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysArticleTagServiceImpl extends ServiceImpl<SysArticleTagMapper, SysArticleTag> implements ISysArticleTagService {

    /**
     * 添加时文章和标签的关联
     * */
    public void addArticleTags(Long articleId, List<Long> tagIds) {
        batchAddTagsForArticle(articleId, tagIds);
    }
    /**
     * 更新添加标签和文章的关联
     * */
    @Transactional
    public void updateArticleTags(Long articleId, List<Long> tagIds) {
        // 删除旧关联
        LambdaQueryWrapper<SysArticleTag> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysArticleTag::getArticleId, articleId);

        remove(queryWrapper);

        batchAddTagsForArticle(articleId, tagIds);
    };

    /**
     * 批量添加标签和文章的关联
     * */
    private void batchAddTagsForArticle(Long articleId, List<Long> tagIds) {
        if (articleId == null) {
            throw new SecurityException("文章 Id为空");
        }
        List<SysArticleTag> list = tagIds.stream()
                .map(tagId -> {
                    SysArticleTag sysArticleTag = new SysArticleTag();
                    sysArticleTag.setTagId(tagId);
                    sysArticleTag.setArticleId(articleId);
                    return sysArticleTag;
                })
                .collect(Collectors.toList());

        saveBatch(list);
    }
}
