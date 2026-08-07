package com.chat.fg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.Friend;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友表 数据层
 * */
@Mapper
public interface FriendMapper extends BaseMapper<Friend> {
}
