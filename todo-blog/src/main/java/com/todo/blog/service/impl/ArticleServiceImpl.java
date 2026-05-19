package com.todo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.todo.blog.pojo.dto.ArticleDTO;
import com.todo.blog.service.IArticleService;
import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysArticle;
import com.todo.common.utils.StringUtils;
import com.todo.system.mapper.SysArticleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ArticleServiceImpl extends ServiceImpl<SysArticleMapper, SysArticle> implements IArticleService {

    /**
     * 查询文章列表
     * */
    @Override
    public PageResult<SysArticle> selectSysArticle(ArticleDTO articleDTO) {

        if (articleDTO.getPageSize() == 0) {
            articleDTO.setPageSize(10);
        }

        if (articleDTO.getPageNum() == 0) {
            articleDTO.setPageNum(1);
        }

        LambdaQueryWrapper<SysArticle> queryWrapper = new LambdaQueryWrapper<>();

        log.debug("queryWrapper:{}", articleDTO.getCategoryId());
        if (articleDTO.getCategoryId() != null && articleDTO.getCategoryId() != 0) {
            queryWrapper.eq(SysArticle::getCategoryId, articleDTO.getCategoryId());
        }

        if (articleDTO.getTitle() != null && !StringUtils.isEmpty(articleDTO.getTitle())) {
            queryWrapper.like(SysArticle::getTitle, articleDTO.getTitle());
        }

        queryWrapper.orderByDesc(SysArticle::getPublishedTime);

        Page<SysArticle> page = this.page(new Page<>(articleDTO.getPageNum(), articleDTO.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据文章 Id查询文章详情
     * */
    @Override
    public SysArticle selectSysArticleById(Long id) {

        if (id == null) {
            throw new SecurityException("文章 Id不能为空");
        }

        LambdaQueryWrapper<SysArticle> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysArticle::getArticleId, id);

        return this.getOne(queryWrapper);
    }

    /**
     * 根据文章 slug查询文章详情
     * */
    @Override
    public SysArticle selectSysArticleBySlug(String slug) {

        if (StringUtils.isEmpty(slug)) {
            throw new SecurityException("slug 不能为空");
        }

        LambdaQueryWrapper<SysArticle> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysArticle::getSlug, slug);

        return this.getOne(queryWrapper);
    }
}
