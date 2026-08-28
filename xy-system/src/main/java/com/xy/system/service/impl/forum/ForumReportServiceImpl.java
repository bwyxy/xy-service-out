package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.dto.forum.ClearReportDTO;
import com.xy.common.entity.forum.ForumReport;
import com.xy.common.utils.SecurityUtils;
import com.xy.system.mapper.forum.ForumReportMapper;
import com.xy.system.service.forum.IForumReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 论坛举报 Service 实现
 */
@Service
public class ForumReportServiceImpl extends ServiceImpl<ForumReportMapper, ForumReport>
        implements IForumReportService {

    /**
     * 获取举报列表
     * */
    @Override
    public PageResult<ForumReport> getForumReportList(ForumReport forumReport) {
        LambdaQueryWrapper<ForumReport> queryWrapper = new LambdaQueryWrapper<>();

        if (forumReport.getTargetType() != null) {
            queryWrapper.eq(ForumReport::getTargetType, forumReport.getTargetType());
        }
        if (forumReport.getStatus() != null) {
            queryWrapper.eq(ForumReport::getStatus, forumReport.getStatus());
        }

        Page<ForumReport> page = this.page(new Page<>(forumReport.getPageNum(), forumReport.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 处理举报
     * */
    @Override
    public void clearReport(ClearReportDTO clearReportDTO) {
        Long uId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<ForumReport> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ForumReport::getReportId, clearReportDTO.getReportId())
                .set(ForumReport::getHandlerId, uId)
                .set(ForumReport::getResult, clearReportDTO.getResult())
                .set(ForumReport::getCreateTime, now);

        update(updateWrapper);
    }
}
