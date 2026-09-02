package com.chat.fg.controller;

import com.chat.fg.pojo.dto.CreateGroupDTO;
import com.chat.fg.service.IGroupService;
import com.xy.common.core.domain.R;
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
     * 创建群聊
     * */
    @PostMapping("/create")
    public R<String> createGroup(@RequestBody CreateGroupDTO createGroupDTO) {

        groupService.createGroup(createGroupDTO);

        return R.ok("添加群聊");
    }
}
