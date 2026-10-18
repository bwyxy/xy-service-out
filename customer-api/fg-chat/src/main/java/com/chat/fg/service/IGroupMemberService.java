package com.chat.fg.service;

import com.chat.fg.pojo.vo.GroupMemberVO;
import com.xy.common.entity.chat.GroupMember;

import java.util.List;

/**
 * 群组成员 逻辑层
 * */
public interface IGroupMemberService {

    /**
     * 批量添加成员
     * */
    void batchGroupMember(Long groupId, List<Long> memberIds);

    /**
     * 根据id查询群成员在群里的拓展信息
     * */
    GroupMember getGroupMemberByUid(Long uId, Long groupId);

    /**
     * 根据群聊 ID查询成员信息
     * */
    List<GroupMemberVO> selectGroupMemberByGroupId(String groupId);
}
