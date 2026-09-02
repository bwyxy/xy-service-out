package com.xy.common.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 好友请求表 t_friend_request
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_friend_request")
public class FriendRequest extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 请求发送者用户ID
     * */
    private Long fromUid;

    /**
     * 请求接收者用户ID
     * */
    private Long toUid;

    /**
     * 状态 0-待处理 1-已同意 2-已拒绝 3-已取消 4-已过期
     * */
    private Integer status;

    /**
     * 申请备注消息
     * */
    private String applyMsg;

    /**
     * 处理回复消息
     * */
    private String replyMsg;
}
