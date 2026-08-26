package com.xy.system.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.forum.ForumOperLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛操作日志 Mapper
 */
@Mapper
public interface ForumOperLogMapper extends BaseMapper<ForumOperLog> {
}
