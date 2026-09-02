package com.xy.common.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话个人设置 t_conversation_setting
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_conversation_setting")
public class ConversationSetting extends BaseEntity {

    /**
     * id
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     * */
    private Long uId;

    /**
     * 频道Id
     * */
    private String channelId;

    /**
     * 频道类型
     * */
    private String channelType;

    /**
     * 是否免打扰
     * */
    private int mute;

    /**
     * 是否置顶
     * */
    private int top;

    /**
     * 草稿
     * */
    private String draft;
}
