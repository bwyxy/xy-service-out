package com.xy.common.vo.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 客户端用户查询条件
 * */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatUserQuery extends BaseEntity {

    /** 用户 UID（精确匹配）
     *  Jackson 从 getUId() 推断出的名字是 uid，读写方法需显式声明为 uId */
    @Getter(onMethod_ = @JsonProperty("uId"))
    @Setter(onMethod_ = @JsonProperty("uId"))
    private Long uId;

    /** 昵称（模糊匹配） */
    private String nickname;

    /** 手机号（模糊匹配） */
    private String phone;

    /** 邮箱（模糊匹配） */
    private String email;

    /** 用户状态 0-正常 1-禁用 */
    private String status;

    /** 在线状态 1-在线 0-离线（依据 Redis 登录会话判定） */
    private String online;

    /** 是否绑定聊天应用 0-未绑定 1-已绑定 */
    private String isBindChat;
}
