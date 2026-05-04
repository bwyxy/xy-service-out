package com.todo.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysArticle;
import com.todo.common.entity.SysBlogConfig;
import com.todo.common.utils.SecurityUtils;
import com.todo.common.utils.StringUtils;
import com.todo.system.mapper.SysArticleMapper;
import com.todo.system.service.ISysArticleService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysArticleServiceImpl extends ServiceImpl<SysArticleMapper, SysArticle> implements ISysArticleService {

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

        LambdaQueryWrapper<SysArticle> queryWrapper = new LambdaQueryWrapper<>();

        if (sysArticle.getTitle() != null && !StringUtils.isEmpty(sysArticle.getTitle())) {
            queryWrapper.like(SysArticle::getTitle, sysArticle.getTitle());
        }

        if (sysArticle.getStatus() != null) {
            queryWrapper.eq(SysArticle::getStatus, sysArticle.getStatus());
        }

        Page<SysArticle> page = this.page(new Page<>(sysArticle.getPageNum(), sysArticle.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据文章 Id获取文章信息
     * */
    @Override
    public SysArticle selectSysArticleById(Long id) {
        if (id == null || StringUtils.isEmpty(id.toString())) {
            throw new SecurityException("文章 Id不能为空");
        }

        LambdaQueryWrapper<SysArticle> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysArticle::getArticleId, id);

        return this.getOne(queryWrapper);
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

        save(sysArticle);
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
    }
}
