package com.chat.fg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.fg.pojo.vo.GroupMemberVO;
import com.xy.common.entity.chat.GroupMember;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 群组成员 数据层
 * */
@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    /**
     * 根据群聊 Id查询成员信息
     * */
    List<GroupMemberVO> selectGroupMemberByGroupId(Long groupId);
}
