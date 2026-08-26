package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumReport;
import com.xy.system.mapper.forum.ForumReportMapper;
import com.xy.system.service.forum.IForumReportService;
import org.springframework.stereotype.Service;

/**
 * 论坛举报 Service 实现
 */
@Service
public class ForumReportServiceImpl extends ServiceImpl<ForumReportMapper, ForumReport>
        implements IForumReportService {
}
