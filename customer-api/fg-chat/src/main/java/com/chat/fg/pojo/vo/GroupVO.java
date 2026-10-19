package com.chat.fg.pojo.vo;

import lombok.Data;

/**
 * 群聊列表项
 */
@Data
public class GroupVO {

    /**
     * 群组 Id
     */
    private Long groupId;

    /**
     * 群组名称
     */
    private String name;

    /**
     * 群头像
     */
    private String avatar;

    /**
     * 群成员数
     */
    private Integer memberCount;

    /**
     * 当前用户在群里的角色 0-成员 1-管理员 2-群主
     */
    private Integer role;
}
