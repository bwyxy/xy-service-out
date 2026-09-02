package com.xy.common.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 群组成员表 t_group_member
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_group_member")
public class GroupMember {

    /**
     * id
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联群组ID
     * */
    private Long groupId;

    /**
     * 用户Id
     * */
    private Long uId;

    /**
     * 角色 0-成员 1-管理员 2-群主
     * */
    private int role;

    /**
     * 状态 1-正常 0-已退出
     * */
    private int status;

    /**
     * 加入时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime joinedAt;

    /**
     * 禁言结束时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime muteEndTime;

    /**
     * 群内昵称
     * */
    private String groupNickname;

    /**
     * 已读到的最大消息seq
     * */
    private Long lastAckSeq;
}
