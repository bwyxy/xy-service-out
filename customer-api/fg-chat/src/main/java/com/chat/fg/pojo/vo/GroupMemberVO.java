package com.chat.fg.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMemberVO {

    private Long id;

    private Long uId;

    private String nickname;

    private String avatar;

    private String sex;

    private Integer role;

    private Integer status;

    private String groupNickname;

    /**
     * 禁言结束时间，为空表示未被禁言
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime muteEndTime;
}
