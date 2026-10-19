package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.mapper.GroupAnnouncementMapper;
import com.chat.fg.pojo.dto.PublishAnnouncementDTO;
import com.chat.fg.pojo.enums.GroupMemberRole;
import com.chat.fg.service.IGroupAnnouncementService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.entity.chat.GroupAnnouncement;
import com.xy.common.entity.chat.GroupMember;
import com.xy.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 群公告 逻辑层
 * */
@Service
public class GroupAnnouncementServiceImpl extends ServiceImpl<GroupAnnouncementMapper, GroupAnnouncement> implements IGroupAnnouncementService {

    @Autowired
    private GroupMemberServiceImpl groupMemberService;

    /**
     * 发布群公告，仅群主/管理员可操作
     * */
    @Override
    public void publishAnnouncement(PublishAnnouncementDTO publishAnnouncementDTO) {
        Long uId = SecurityUtils.getUId();

        checkAdminPermission(uId, publishAnnouncementDTO.getGroupId());

        GroupAnnouncement announcement = new GroupAnnouncement();
        announcement.setGroupId(publishAnnouncementDTO.getGroupId());
        announcement.setTitle(publishAnnouncementDTO.getTitle());
        announcement.setContent(publishAnnouncementDTO.getContent());
        announcement.setPublisherUid(uId);
        announcement.setIsPinned(publishAnnouncementDTO.getIsPinned() == null ? 0 : publishAnnouncementDTO.getIsPinned());

        save(announcement);
    }

    /**
     * 查询群公告列表，置顶优先、按发布时间倒序
     * */
    @Override
    public List<GroupAnnouncement> listAnnouncements(Long groupId) {
        if (groupId == null) {
            throw new ServiceException("群聊Id不能为空");
        }
        LambdaQueryWrapper<GroupAnnouncement> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(GroupAnnouncement::getGroupId, groupId)
                .orderByDesc(GroupAnnouncement::getIsPinned)
                .orderByDesc(GroupAnnouncement::getCreateTime);

        return list(queryWrapper);
    }

    /**
     * 删除群公告，仅群主/管理员可操作
     * */
    @Override
    public void deleteAnnouncement(Long groupId, Long id) {
        if (groupId == null || id == null) {
            throw new ServiceException("群聊Id和公告Id不能为空");
        }
        Long uId = SecurityUtils.getUId();

        checkAdminPermission(uId, groupId);

        LambdaQueryWrapper<GroupAnnouncement> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(GroupAnnouncement::getId, id)
                .eq(GroupAnnouncement::getGroupId, groupId);

        remove(queryWrapper);
    }

    /**
     * 校验当前用户在群内是否具备管理员权限
     * */
    private void checkAdminPermission(Long uId, Long groupId) {
        GroupMember groupMember = groupMemberService.getGroupMemberByUid(uId, groupId);

        if (groupMember == null || groupMember.getRole() == GroupMemberRole.regular.getCode()) {
            throw new ServiceException("您没有权限操作群公告");
        }
    }
}
