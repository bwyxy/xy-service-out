package com.chat.fg.service;

import com.chat.fg.pojo.dto.GroupNicknameDTO;
import com.chat.fg.pojo.dto.MuteMemberDTO;
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

    /**
     * 群成员禁言/解禁
     * */
    void muteMember(MuteMemberDTO muteMemberDTO);

    /**
     * 修改当前用户在群内的昵称
     * */
    void updateGroupNickname(GroupNicknameDTO groupNicknameDTO);
}
