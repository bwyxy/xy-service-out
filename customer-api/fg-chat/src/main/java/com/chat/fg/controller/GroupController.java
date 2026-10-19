package com.chat.fg.controller;

import com.chat.fg.pojo.dto.CreateGroupDTO;
import com.chat.fg.pojo.dto.EditGroupDTO;
import com.chat.fg.pojo.dto.GroupNicknameDTO;
import com.chat.fg.pojo.dto.MuteMemberDTO;
import com.chat.fg.pojo.dto.PublishAnnouncementDTO;
import com.chat.fg.pojo.vo.GroupMemberVO;
import com.chat.fg.service.IGroupAnnouncementService;
import com.chat.fg.service.IGroupMemberService;
import com.chat.fg.service.IGroupService;
import com.xy.common.core.domain.R;
import com.xy.common.entity.chat.Group;
import com.xy.common.entity.chat.GroupAnnouncement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 群聊 控制层
 * */
@RestController
@RequestMapping("/group")
public class GroupController {

    @Autowired
    private IGroupService groupService;

    @Autowired
    private IGroupMemberService groupMemberService;

    @Autowired
    private IGroupAnnouncementService groupAnnouncementService;

    /**
     * 查询群聊信息
     * */
    @GetMapping("/{id}")
    public R<Group> selectById(@PathVariable String id) {
        Group group = groupService.findById(id);

        return R.ok(group);
    }

    /**
     * 创建群聊
     * */
    @PostMapping("/create")
    public R<String> createGroup(@RequestBody CreateGroupDTO createGroupDTO) {

        groupService.createGroup(createGroupDTO);

        return R.ok("添加群聊成功");
    }

    /**
     * 查询群成员信息
     * */
    @GetMapping("/{groupId}/members")
    public R<List<GroupMemberVO>> selectGroupMemberByGroupId(@PathVariable String groupId) {
        List<GroupMemberVO> members = groupMemberService.selectGroupMemberByGroupId(groupId);

        return R.ok(members);
    }

    /**
     * 编辑群聊信息（名称/头像/全员禁言/历史消息可见）
     * */
    @PutMapping("/edit")
    public R<String> editGroup(@RequestBody EditGroupDTO editGroupDTO) {
        groupService.editGroup(editGroupDTO);

        return R.ok("编辑成功");
    }

    /**
     * 修改当前用户在群内的昵称
     * */
    @PutMapping("/member/nickname")
    public R<String> updateGroupNickname(@Valid @RequestBody GroupNicknameDTO groupNicknameDTO) {
        groupMemberService.updateGroupNickname(groupNicknameDTO);

        return R.ok("修改成功");
    }

    /**
     * 群成员禁言/解禁
     * */
    @PutMapping("/member/mute")
    public R<String> muteMember(@Valid @RequestBody MuteMemberDTO muteMemberDTO) {
        groupMemberService.muteMember(muteMemberDTO);

        return R.ok("操作成功");
    }

    /**
     * 发布群公告
     * */
    @PostMapping("/announcement")
    public R<String> publishAnnouncement(@Valid @RequestBody PublishAnnouncementDTO publishAnnouncementDTO) {
        groupAnnouncementService.publishAnnouncement(publishAnnouncementDTO);

        return R.ok("发布成功");
    }

    /**
     * 查询群公告列表
     * */
    @GetMapping("/{groupId}/announcement")
    public R<List<GroupAnnouncement>> listAnnouncements(@PathVariable Long groupId) {
        List<GroupAnnouncement> announcements = groupAnnouncementService.listAnnouncements(groupId);

        return R.ok(announcements);
    }

    /**
     * 删除群公告
     * */
    @DeleteMapping("/{groupId}/announcement/{id}")
    public R<String> deleteAnnouncement(@PathVariable Long groupId, @PathVariable Long id) {
        groupAnnouncementService.deleteAnnouncement(groupId, id);

        return R.ok("删除成功");
    }

    /**
     * 解散群聊
     * */
    @DeleteMapping("/{id}")
    public R<String> dissolveGroup(@PathVariable Long id) {

        groupService.dissolveGroup(id);

        return R.ok("群聊已解散");
    }
}
