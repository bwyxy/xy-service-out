package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysContentItem;
import com.xy.common.enums.ContentItemType;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysContentItemMapper;
import com.xy.system.service.ISysContentItemService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysContentItemServiceImpl extends ServiceImpl<SysContentItemMapper, SysContentItem> implements ISysContentItemService {

    /**
     * 分页查询内容通用项
     * */
    @Override
    public PageResult<SysContentItem> selectContentItems(SysContentItem sysContentItem) {
        if (sysContentItem.getPageNum() == 0) {
            sysContentItem.setPageNum(1);
        }

        if (sysContentItem.getPageSize() == 0) {
            sysContentItem.setPageSize(10);
        }

        LambdaQueryWrapper<SysContentItem> queryWrapper = new LambdaQueryWrapper<>();

        if (sysContentItem.getTitle() != null && !StringUtils.isEmpty(sysContentItem.getTitle())) {
            queryWrapper.like(SysContentItem::getTitle, sysContentItem.getTitle());
        }

        if (sysContentItem.getType() != null && !StringUtils.isEmpty(sysContentItem.getType())) {
            queryWrapper.eq(SysContentItem::getType, sysContentItem.getType());
        }

        queryWrapper
                .eq(SysContentItem::getDeleted, 0)
                .orderByAsc(SysContentItem::getSortOrder);

        Page<SysContentItem> page = this.page(new Page<>(sysContentItem.getPageNum(), sysContentItem.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据内容配置 Id查询详情
     * */
    @Override
    public SysContentItem selectContentItemById(Long id) {
        if (id == null) {
            throw new SecurityException("id is null");
        }

        LambdaQueryWrapper<SysContentItem> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysContentItem::getContentId, id);

        return this.getOne(queryWrapper);
    }
    /**
     * 添加内容通用项
     * */
    @Override
    public void addContentItem(SysContentItem sysContentItem) {
        ContentItemType contentItemType = verificationContentItemParams(sysContentItem);

        save(sysContentItem);
    };

    /**
     * 更新内容通用项
     * */
    @Override
    public void updateContentItem(SysContentItem sysContentItem) {
        if (sysContentItem.getContentId() == null) {
            throw new SecurityException("id 不能为空");
        }

        verificationContentItemParams(sysContentItem);

        updateById(sysContentItem);
    }

    /**
     * 根据内容配置 Id删除
     * */
    @Override
    public void deleteContentItemById(Long id) {
        if (id == null) {
            throw new SecurityException("id is null");
        }
        LambdaUpdateWrapper<SysContentItem> updateWrapper = new LambdaUpdateWrapper<>();

        LocalDateTime now = LocalDateTime.now();

        updateWrapper.eq(SysContentItem::getContentId, id)
                .set(SysContentItem::getDeleted, 1)
                .set(SysContentItem::getUpdateTime, now);

        update(null, updateWrapper);
    }

    /**
     * 校验内容通用项参数
     * */
    private ContentItemType verificationContentItemParams(SysContentItem sysContentItem) {
        if (sysContentItem.getType() == null || StringUtils.isEmpty(sysContentItem.getType())) {
            throw new SecurityException("内容通用项类型不能为空");
        }

        if (sysContentItem.getTitle()  == null || StringUtils.isEmpty(sysContentItem.getTitle())) {
            throw new SecurityException("标题不能为空");
        }

        return ContentItemType.valueOf(sysContentItem.getType());
    }
}
