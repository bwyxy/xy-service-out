package com.chat.fg.pojo.vo;

import lombok.Data;

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
}
