package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumOperLog;
import com.xy.system.mapper.forum.ForumOperLogMapper;
import com.xy.system.service.forum.IForumOperLogService;
import org.springframework.stereotype.Service;

/**
 * 论坛操作日志 Service 实现
 */
@Service
public class ForumOperLogServiceImpl extends ServiceImpl<ForumOperLogMapper, ForumOperLog>
        implements IForumOperLogService {
}
