package com.xy.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.chat.UserBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户黑名单 Mapper
 * */
@Mapper
public interface UserBlacklistMapper extends BaseMapper<UserBlacklist> {
}
