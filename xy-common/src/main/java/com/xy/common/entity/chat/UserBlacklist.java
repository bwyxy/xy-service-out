package com.xy.common.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户黑名单 t_user_blacklist
 *
 * <p>拉黑关系为用户对用户且单向：uId 拉黑 targetUid 后，被拉黑方向拉黑方发送的消息会被拒绝投递。
 * 后台管理端作为运营视角可代加/代删。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_blacklist")
public class UserBlacklist extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 拉黑发起方 UID
     *  Jackson 从 getUId()/setUId() 推断出的名字是 uid，读写方法需显式声明为 uId */
    @Getter(onMethod_ = @JsonProperty("uId"))
    @Setter(onMethod_ = @JsonProperty("uId"))
    @TableField("u_id")
    private Long uId;

    /** 被拉黑用户 UID */
    @TableField("target_uid")
    private Long targetUid;

    /** 备注 */
    private String remark;

    /** 操作人（后台代管时记录管理员账号） */
    private String operator;
}
