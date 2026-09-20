package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysArticle;
import com.xy.common.entity.SysBlogConfig;
import com.xy.common.utils.SecurityUtils;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysArticleMapper;
import com.xy.system.mapper.SysArticleTagMapper;
import com.xy.system.service.ISysArticleService;
import com.xy.system.service.ISysArticleTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysArticleServiceImpl extends ServiceImpl<SysArticleMapper, SysArticle> implements ISysArticleService {

    @Autowired
    private ISysArticleTagService sysArticleTagService;

    @Autowired
    private SysArticleTagMapper sysArticleTagMapper;

    /**
     * 分页查询文章列表
     * */
    @Override
    public PageResult<SysArticle> selectSysArticleList(SysArticle sysArticle) {

        if (sysArticle.getPageNum() == 0) {
            sysArticle.setPageNum(1);
        }

        if (sysArticle.getPageSize() == 0) {
            sysArticle.setPageSize(10);
        }

        Page<SysArticle> page = new Page<>(sysArticle.getPageNum(), sysArticle.getPageSize());
        List<SysArticle> records = baseMapper.selectSysArticleList(page, sysArticle);

        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据文章 Id获取文章信息
     * */
    @Override
    public SysArticle selectSysArticleById(Long id) {
        if (id == null || StringUtils.isEmpty(id.toString())) {
            throw new SecurityException("文章 Id不能为空");
        }

        SysArticle article = baseMapper.selectSysArticleById(id);

        if (article != null) {
            LambdaQueryWrapper<com.xy.common.entity.SysArticleTag> tagQueryWrapper = new LambdaQueryWrapper<>();
            tagQueryWrapper.eq(com.xy.common.entity.SysArticleTag::getArticleId, id);
            List<Long> tagIds = sysArticleTagMapper.selectList(tagQueryWrapper)
                    .stream()
                    .map(com.xy.common.entity.SysArticleTag::getTagId)
                    .collect(Collectors.toList());
            article.setTagIds(tagIds);
        }

        return article;
    }

    /**
     * 新增文章
     * */
    @Override
    public void addSysArticle(SysArticle sysArticle) {

        Long userId = SecurityUtils.getUserId();

        if (sysArticle.getTitle() == null || StringUtils.isEmpty(sysArticle.getTitle())) {
            throw new SecurityException("文章标题不能为空");
        }

        sysArticle.setAuthorId(userId);

        boolean success = save(sysArticle);

        if (success) {
            // 获取文章 Id
            Long ArticleId = sysArticle.getArticleId();
            if (sysArticle.getTagIds() != null && !sysArticle.getTagIds().isEmpty()) {
                // 添加文章和标签的关系
                sysArticleTagService.addArticleTags(ArticleId, sysArticle.getTagIds());
            }
        }
    }

    /**
     * 更新文章
     * */
    public void updateSysArticle(SysArticle sysArticle) {

        if (sysArticle.getArticleId() == null) {
            throw new SecurityException("文章 Id不能为空");
        }

        LambdaUpdateWrapper<SysArticle> updateWrapper = new LambdaUpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now();

        updateWrapper.eq(SysArticle::getArticleId, sysArticle.getArticleId());

        if (sysArticle.getTitle() != null && !StringUtils.isEmpty(sysArticle.getTitle())) {
            updateWrapper.set(SysArticle::getTitle, sysArticle.getTitle());
        }

        updateWrapper.set(SysArticle::getUpdateTime, now);

        update(sysArticle, updateWrapper);

        // 更新文章和标签的关系
        sysArticleTagService.updateArticleTags(sysArticle.getArticleId(), sysArticle.getTagIds());
    }

    /** */
}
