package com.todo.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysArticle;
import com.todo.common.utils.StringUtils;
import com.todo.system.mapper.SysArticleMapper;
import com.todo.system.service.ISysArticleService;
import org.springframework.stereotype.Service;

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
}
