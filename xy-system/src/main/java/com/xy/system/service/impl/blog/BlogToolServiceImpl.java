package com.xy.system.service.impl.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.blog.BlogTool;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.blog.BlogToolMapper;
import com.xy.system.service.blog.IBlogToolService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BlogToolServiceImpl extends ServiceImpl<BlogToolMapper, BlogTool> implements IBlogToolService {

    /**
     * 分页查询工具列表
     * */
    @Override
    public PageResult<BlogTool> getBlogToolList(BlogTool blogTool) {
        LambdaQueryWrapper<BlogTool> queryWrapper = new LambdaQueryWrapper<>();

        if (blogTool.getName() != null && !StringUtils.isEmpty(blogTool.getName())) {
            queryWrapper.like(BlogTool::getName, blogTool.getName());
        }

        if (blogTool.getCategoryId() != null) {
            queryWrapper.eq(BlogTool::getCategoryId, blogTool.getCategoryId());
        }

        Page<BlogTool> page = this.page(new Page<>(blogTool.getPageNum(), blogTool.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据id 查询工具信息
     * */
    @Override
    public BlogTool getBlogToolById(Long id) {
        if (id==null) {
            throw new ServiceException("id 不能为空");
        }

        return baseMapper.selectById(id);
    }

    /**
     * 添加工具
     * */
    @Override
    public void addBlogTool(BlogTool blogTool) {
        if (blogTool.getName() == null || StringUtils.isEmpty(blogTool.getName())) {
            throw new ServiceException("工具名称不能为空");
        }
        if (blogTool.getUrl() == null || StringUtils.isEmpty(blogTool.getUrl())) {
            throw new ServiceException("地址不能为空");
        }
        save(blogTool);
    }

    /**
     * 更新工具
     * */
    @Override
    public void updateBlogTool(BlogTool blogTool) {
        if (blogTool.getId() == null) {
            throw new ServiceException("id 不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<BlogTool> updateWrapper = new LambdaUpdateWrapper<>();

        if (blogTool.getName() != null && !StringUtils.isEmpty(blogTool.getName())) {
            updateWrapper.set(BlogTool::getName, blogTool.getName());
        }
        if (blogTool.getUrl() != null && !StringUtils.isEmpty(blogTool.getUrl())) {
            updateWrapper.set(BlogTool::getUrl, blogTool.getUrl());
        }

        updateWrapper.eq(BlogTool::getId, blogTool.getId())
                .set(BlogTool::getIcon, blogTool.getIcon())
                .set(BlogTool::getDescription, blogTool.getDescription())
                .set(BlogTool::getPlatform, blogTool.getPlatform())
                .set(BlogTool::getPriceType, blogTool.getPriceType())
                .set(BlogTool::getSort, blogTool.getSort())
                .set(BlogTool::getStatus, blogTool.getStatus())
                .set(BlogTool::getIsRecommend, blogTool.getIsRecommend())
                .set(BlogTool::getUpdateTime, now);

        update(updateWrapper);
    }

    /**
     * 删除工具
     * */
    @Override
    public void deleteBlogTool(Long id) {
        if (id == null) {
            throw new ServiceException("id 不能为空");
        }
        baseMapper.deleteById(id);
    }
}
