package com.xy.system.service.impl.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.blog.BlogNotice;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.blog.BlogNoticeMapper;
import com.xy.system.service.blog.IBlogNoticeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BlogNoticeServiceImpl extends ServiceImpl<BlogNoticeMapper, BlogNotice> implements IBlogNoticeService {

    /**
     * 分页查询公告列表
     * */
    @Override
    public PageResult<BlogNotice> selectBlogNotice(BlogNotice blogNotice) {
        LambdaQueryWrapper<BlogNotice> wrapper = new LambdaQueryWrapper<>();

        String title = blogNotice.getTitle();

        if (title != null && !StringUtils.isEmpty(title)) {
            wrapper.like(BlogNotice::getTitle, title);
        }
        if (blogNotice.getType() != null) {
            wrapper.eq(BlogNotice::getType, blogNotice.getType());
        }
        if (blogNotice.getStatus() != null) {
            wrapper.eq(BlogNotice::getStatus, blogNotice.getStatus());
        }

        Page<BlogNotice> page = this.page(new Page<>(blogNotice.getPageNum(), blogNotice.getPageSize()), wrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 查询公告详情
     * */
    @Override
    public BlogNotice selectBlogNoticeById(Long id) {
        if (id == null) {
            throw new ServiceException("公告Id 不能为空");
        }
        return this.baseMapper.selectById(id);
    }

    /**
     * 添加公告
     * */
    @Override
    public void addBlogNotice(BlogNotice blogNotice) {
        if (blogNotice.getTitle() == null || StringUtils.isEmpty(blogNotice.getTitle())) {
            throw new ServiceException("标题不能为空");
        }
        if (blogNotice.getContent() == null || StringUtils.isEmpty(blogNotice.getContent())) {
            throw new ServiceException("内容不能为空");
        }
        save(blogNotice);
    }

    /**
     * 更新公告
     * */
    @Override
    public void updateBlogNotice(BlogNotice blogNotice) {
        if (blogNotice.getId() == null) {
            throw new ServiceException("公告Id 不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<BlogNotice> wrapper = new LambdaUpdateWrapper<>();

        if (blogNotice.getTitle() != null && !StringUtils.isEmpty(blogNotice.getTitle())) {
            wrapper.set(BlogNotice::getTitle, blogNotice.getTitle());
        }
        if (blogNotice.getContent() != null && !StringUtils.isEmpty(blogNotice.getContent())) {
            wrapper.set(BlogNotice::getContent, blogNotice.getContent());
        }
        if (blogNotice.getStatus() != null) {
            wrapper.set(BlogNotice::getStatus, blogNotice.getStatus());
        }
        if (blogNotice.getType() != null) {
            wrapper.set(BlogNotice::getType, blogNotice.getType());
        }
        if (blogNotice.getLinkUrl() != null && !StringUtils.isEmpty(blogNotice.getLinkUrl())) {
            wrapper.set(BlogNotice::getLinkUrl, blogNotice.getLinkUrl());
        }
        wrapper.set(BlogNotice::getIsTop, blogNotice.getIsTop())
                .set(BlogNotice::getSort, blogNotice.getSort())
                .set(BlogNotice::getStartTime, blogNotice.getStartTime())
                .set(BlogNotice::getEndTime, blogNotice.getEndTime())
                .set(BlogNotice::getUpdateTime, now);

        update(wrapper);
    }

    /**
     * 删除公告
     * */
    @Override
    public void deleteBlogNotice(Long id) {
        if (id == null) {
            throw new ServiceException("公告Id 不能为空");
        }

        LambdaQueryWrapper<BlogNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogNotice::getId, id);

        remove(wrapper);
    }
}
