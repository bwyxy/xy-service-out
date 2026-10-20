package com.chat.fg.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 群通话邀请结果 VO
 */
@Data
public class InviteVO {

    /**
     * 房间 ID
     */
    private String roomId;

    /**
     * 房间人数上限
     */
    private Integer maxParticipants;

    /**
     * 本次邀请后的房间成员
     */
    private List<RtcMemberVO> participants = new ArrayList<>();
}
