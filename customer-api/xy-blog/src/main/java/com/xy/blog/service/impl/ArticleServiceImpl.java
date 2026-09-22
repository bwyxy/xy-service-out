package com.xy.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.dto.blog.ArticleDTO;
import com.xy.blog.service.IArticleService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysArticle;
import com.xy.common.utils.StringUtils;
import com.xy.common.vo.blog.ArticleVO;
import com.xy.system.mapper.SysArticleMapper;
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
    public PageResult<ArticleVO> selectSysArticle(ArticleDTO articleDTO) {

        Page<ArticleVO> page = new Page<>(articleDTO.getPageNum(), articleDTO.getPageSize());

        List<ArticleVO> records = baseMapper.selectArticleVOList(page, articleDTO);

        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
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
