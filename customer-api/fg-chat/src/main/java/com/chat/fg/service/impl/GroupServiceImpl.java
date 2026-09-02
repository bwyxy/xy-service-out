package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.pojo.dto.CreateGroupDTO;
import com.chat.fg.service.IGroupService;
import com.xy.common.entity.chat.Group;
import com.xy.system.mapper.GroupMapper;
import org.springframework.stereotype.Service;

/**
 * 群聊 逻辑层
 * */
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements IGroupService {

    /**
     * 创建群聊
     */
    @Override
    public void createGroup(CreateGroupDTO createGroupDTO) {}
}
