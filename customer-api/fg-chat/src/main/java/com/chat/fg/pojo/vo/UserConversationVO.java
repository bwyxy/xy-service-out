package com.chat.fg.pojo.vo;

import com.xy.common.client.vo.ConversationVO;
import lombok.Data;

/**
 * 用户会话
 * */
@Data
public class UserConversationVO extends ConversationVO {

    /**
     * 名称
     * */
    private String name;

    /**
     * 头像
     * */
    private String avatar;

    /**
     * 是否是系统账号
     * */
    private boolean system;

    /**
     * 是否置顶 (1=置顶, 0=未置顶)
     * */
    private Integer top;
}
