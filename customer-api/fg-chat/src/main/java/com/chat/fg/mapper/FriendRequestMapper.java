package com.chat.fg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友请求表 数据层
 * */
@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
}
