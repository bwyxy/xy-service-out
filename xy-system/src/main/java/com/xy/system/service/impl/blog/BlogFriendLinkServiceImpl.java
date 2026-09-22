package com.xy.system.service.impl.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.blog.BlogFriendLink;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.blog.BlogFriendLinkMapper;
import com.xy.system.service.blog.IBlogFriendLinkService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BlogFriendLinkServiceImpl extends ServiceImpl<BlogFriendLinkMapper, BlogFriendLink> implements IBlogFriendLinkService {

    /**
     * 分页查询友链列表
     * */
    @Override
    public PageResult<BlogFriendLink> selectBlogFriendListByPage(BlogFriendLink blogFriendLink) {
        LambdaQueryWrapper<BlogFriendLink> queryWrapper = new LambdaQueryWrapper<>();

        if (blogFriendLink.getName() != null && !StringUtils.isEmpty(blogFriendLink.getName())) {
            queryWrapper.like(BlogFriendLink::getName, blogFriendLink.getName());
        }

        Page<BlogFriendLink> page = this.page(new Page<>(blogFriendLink.getPageNum(), blogFriendLink.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据id 查询友链详情
     * */
    @Override
    public BlogFriendLink selectBlogFriendLinkById(Long id) {
        if (id == null) {
            throw new ServiceException("友链Id 不能为空");
        }
        return this.baseMapper.selectById(id);
    }

    /**
     * 添加友链
     * */
    @Override
    public void insertBlogFriendLink(BlogFriendLink blogFriendLink) {
        if (blogFriendLink.getName() == null || StringUtils.isEmpty(blogFriendLink.getName())) {
            throw new ServiceException("站点名称不能为空");
        }
        if (blogFriendLink.getUrl() == null || StringUtils.isEmpty(blogFriendLink.getUrl())) {
            throw new ServiceException("站点地址不能为空");
        }
        save(blogFriendLink);
    }

    /**
     * 编辑友链
     * */
    @Override
    public void updateBlogFriendLink(BlogFriendLink blogFriendLink) {
        if (blogFriendLink.getId() == null) {
            throw new ServiceException("友链Id 不能为空");
        }
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<BlogFriendLink> updateWrapper = new LambdaUpdateWrapper<>();

        if (blogFriendLink.getName() != null && !StringUtils.isEmpty(blogFriendLink.getName())) {
            updateWrapper.set(BlogFriendLink::getName, blogFriendLink.getName());
        }
        if (blogFriendLink.getUrl() != null && !StringUtils.isEmpty(blogFriendLink.getUrl())) {
            updateWrapper.set(BlogFriendLink::getUrl, blogFriendLink.getUrl());
        }
        if (blogFriendLink.getLogo() != null && !StringUtils.isEmpty(blogFriendLink.getLogo())) {
            updateWrapper.set(BlogFriendLink::getLogo, blogFriendLink.getLogo());
        }
        if (blogFriendLink.getDescription() != null && !StringUtils.isEmpty(blogFriendLink.getDescription())) {
            updateWrapper.set(BlogFriendLink::getDescription, blogFriendLink.getDescription());
        }
        if (blogFriendLink.getEmail() != null && !StringUtils.isEmpty(blogFriendLink.getEmail())) {
            updateWrapper.set(BlogFriendLink::getEmail, blogFriendLink.getEmail());
        }
        updateWrapper.set(BlogFriendLink::getSort, blogFriendLink.getSort())
                .set(BlogFriendLink::getStatus, blogFriendLink.getStatus())
                .set(BlogFriendLink::getRemark, blogFriendLink.getRemark())
                .set(BlogFriendLink::getUpdateTime, now)
                .eq(BlogFriendLink::getId, blogFriendLink.getId());

        update(updateWrapper);
    }

    /**
     * 删除友链
     * */
    @Override
    public void deleteBlogFriendLinkById(Long id) {
        if (id == null) {
            throw new ServiceException("友链Id 不能为空");
        }

        LambdaQueryWrapper<BlogFriendLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BlogFriendLink::getId, id);

        remove(queryWrapper);
    }
}
