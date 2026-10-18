package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.mapper.GroupMemberMapper;
import com.chat.fg.pojo.vo.GroupMemberVO;
import com.chat.fg.service.IGroupMemberService;
import com.xy.common.entity.chat.GroupMember;
import com.xy.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 群组成员 逻辑层
 * */
@Service
public class GroupMemberServiceImpl extends ServiceImpl<GroupMemberMapper, GroupMember> implements IGroupMemberService {

    /**
     * 根据id查询群成员在群里的拓展信息
     * */
    @Override
    public GroupMember getGroupMemberByUid(Long uId, Long groupId) {
        if (uId == null || groupId == null) {
            throw new ServiceException("用户Id和群聊Id不能为空");
        }
        LambdaQueryWrapper<GroupMember> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUId, uId);

        return this.getOne(queryWrapper);
    }

    /**
     * 根据群聊 ID查询成员信息
     * */
    @Override
    public List<GroupMemberVO> selectGroupMemberByGroupId(String groupId) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(groupId);

        if (matcher.find()) {
            Long id = Long.valueOf(matcher.group());

            return baseMapper.selectGroupMemberByGroupId(id);
        }
        throw new ServiceException("无效群聊 ID");
    }

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
