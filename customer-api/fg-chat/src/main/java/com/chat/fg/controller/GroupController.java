package com.chat.fg.controller;

import com.chat.fg.pojo.dto.CreateGroupDTO;
import com.chat.fg.pojo.dto.EditGroupDTO;
import com.chat.fg.service.IGroupService;
import com.xy.common.core.domain.R;
import com.xy.common.entity.chat.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 群聊 控制层
 * */
@RestController
@RequestMapping("/group")
public class GroupController {

    @Autowired
    private IGroupService groupService;

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
     * 编辑群聊信息
     * */
    @PutMapping("/edit")
    public R<String> editGroup(@RequestBody EditGroupDTO editGroupDTO) {
        groupService.editGroup(editGroupDTO);

        return R.ok("编辑成功");
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
