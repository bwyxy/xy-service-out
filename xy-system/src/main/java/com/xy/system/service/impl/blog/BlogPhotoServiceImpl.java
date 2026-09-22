package com.xy.system.service.impl.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.blog.BlogPhoto;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.blog.BlogPhotoMapper;
import com.xy.system.service.blog.IBlogPhotoService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BlogPhotoServiceImpl extends ServiceImpl<BlogPhotoMapper, BlogPhoto> implements IBlogPhotoService {

    /**
     * 分页查询照片列表
     * */
    @Override
    public PageResult<BlogPhoto> getBlogPhotoByPage(BlogPhoto blogPhoto) {
        LambdaQueryWrapper<BlogPhoto> queryWrapper = new LambdaQueryWrapper<>();

        if (blogPhoto.getName() != null && !StringUtils.isEmpty(blogPhoto.getName())) {
            queryWrapper.like(BlogPhoto::getName, blogPhoto.getName());
        }

        if (blogPhoto.getCategoryId() != null) {
            queryWrapper.eq(BlogPhoto::getCategoryId, blogPhoto.getCategoryId());
        }

        if (blogPhoto.getStatus() != null) {
            queryWrapper.eq(BlogPhoto::getStatus, blogPhoto.getStatus());
        }

        Page<BlogPhoto> page = this.page(new Page<>(blogPhoto.getPageNum(), blogPhoto.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据id 查询照片信息
     * */
    @Override
    public BlogPhoto getBlogPhotoById(Long id) {
        if (id == null) {
            throw new ServiceException("id 不能为空");
        }
        return baseMapper.selectById(id);
    }

    /**
     * 添加照片
     * */
    @Override
    public void addBlogPhoto(BlogPhoto blogPhoto) {
        validParam(blogPhoto);

        save(blogPhoto);
    }

    /**
     * 更新照片
     * */
    @Override
    public void updateBlogPhoto(BlogPhoto blogPhoto) {
        if (blogPhoto.getId() == null) {
            throw new ServiceException("id 不能为空");
        }
        validParam(blogPhoto);

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<BlogPhoto> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(BlogPhoto::getId, blogPhoto.getId())
                .set(BlogPhoto::getCategoryId, blogPhoto.getCategoryId())
                .set(BlogPhoto::getName, blogPhoto.getName())
                .set(BlogPhoto::getPhotoUrl, blogPhoto.getPhotoUrl())
                .set(BlogPhoto::getStatus, blogPhoto.getStatus())
                .set(BlogPhoto::getDescription, blogPhoto.getDescription())
                .set(BlogPhoto::getSort, blogPhoto.getSort())
                .set(BlogPhoto::getPublishedTime, blogPhoto.getPublishedTime())
                .set(BlogPhoto::getUpdateTime, now);

        update(updateWrapper);
    }

    private void validParam(BlogPhoto blogPhoto) {
        if (blogPhoto.getName() == null || StringUtils.isEmpty(blogPhoto.getName())) {
            throw new ServiceException("照片名称不能为空");
        }
        if (blogPhoto.getPhotoUrl() == null || StringUtils.isEmpty(blogPhoto.getPhotoUrl())) {
            throw new ServiceException("照片地址不能为空");
        }
    }

    /**
     * 删除照片
     * */
    @Override
    public void deleteBlogPhoto(Long id) {
        if (id == null) {
            throw new ServiceException("id 不能为空");
        }
        baseMapper.deleteById(id);
    }
}
