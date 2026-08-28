package com.xy.system.service.forum;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.dto.forum.ClearReportDTO;
import com.xy.common.entity.forum.ForumReport;

/**
 * 论坛举报 Service 接口
 */
public interface IForumReportService extends IService<ForumReport> {

    /**
     * 获取举报列表
     * */
    PageResult<ForumReport> getForumReportList(ForumReport forumReport);

    /**
     * 处理举报
     * */
    void clearReport(ClearReportDTO clearReportDTO);
}
