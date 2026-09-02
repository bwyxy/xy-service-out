package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.mapper.GroupMemberMapper;
import com.chat.fg.service.IGroupMemberService;
import com.xy.common.entity.chat.GroupMember;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 群组成员 逻辑层
 * */
@Service
public class GroupMemberServiceImpl extends ServiceImpl<GroupMemberMapper, GroupMember> implements IGroupMemberService {

    /**
     * 批量添加成员
     * */
    @Override
    public void batchGroupMember(Long groupId, List<Long> memberIds) {

        LocalDateTime now = LocalDateTime.now();
        List<GroupMember> groupMembers = new ArrayList<>(memberIds.size());

        for (Long memberId : memberIds) {
            GroupMember groupMember = new GroupMember();

            groupMember.setGroupId(groupId);
            groupMember.setUId(memberId);
            groupMember.setRole(0);
            groupMember.setJoinedAt(now);

            groupMembers.add(groupMember);
        }

        saveBatch(groupMembers);
    }
}
