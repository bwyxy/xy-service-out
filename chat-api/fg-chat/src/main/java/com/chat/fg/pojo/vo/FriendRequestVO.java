package com.chat.fg.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友请求 VO
 * */
@Data
public class FriendRequestVO {

    /**
     * 请求ID
     * */
    private Long id;

    /**
     * 发送者ID
     * */
    private Long fromUid;

    /**
     * 发送者昵称
     * */
    private String fromNickname;

    /**
     * 发送者头像
     * */
    private String fromAvatar;

    /**
     * 接收者ID
     * */
    private Long toUid;

    /**
     * 状态 0-待处理 1-已同意 2-已拒绝 3-已取消 4-已过期
     * */
    private Integer status;

    /**
     * 状态描述
     * */
    private String statusDesc;

    /**
     * 申请备注
     * */
    private String applyMsg;

    /**
     * 回复消息
     * */
    private String replyMsg;

    /**
     * 创建时间
     * */
    private LocalDateTime createTime;
}
