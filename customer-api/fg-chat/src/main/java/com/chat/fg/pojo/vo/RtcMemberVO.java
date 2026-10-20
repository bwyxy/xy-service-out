package com.chat.fg.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通话成员 VO
 */
@Data
public class RtcMemberVO {

    /**
     * 用户 ID
     */
    private Long uid;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 群内昵称（群聊才有）
     */
    private String groupNickname;

    /**
     * 房间角色 0-普通 1-主持人
     */
    private Integer role;

    /**
     * 参与者状态 0-呼叫中(已邀请未接入) 1-已加入 2-已离开 3-拒绝 4-忙线
     */
    private Integer status;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;
}
