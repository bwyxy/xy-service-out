package com.chat.fg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.chat.GroupMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群组成员 数据层
 * */
@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
}
