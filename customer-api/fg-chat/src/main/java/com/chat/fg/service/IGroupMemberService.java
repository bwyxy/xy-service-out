package com.chat.fg.service;

import java.util.List;

/**
 * 群组成员 逻辑层
 * */
public interface IGroupMemberService {

    /**
     * 批量添加成员
     * */
    void batchGroupMember(Long groupId, List<Long> memberIds);
}
