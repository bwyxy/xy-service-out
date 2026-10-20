package com.xy.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.chat.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户端用户登录日志 Mapper
 * */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
