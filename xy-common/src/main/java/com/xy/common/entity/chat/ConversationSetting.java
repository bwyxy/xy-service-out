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
    private Integer channelType;

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

    /**
     * 删除聊天记录时的消息序号（仅当前用户可见范围，小于等于该序号的消息不再展示）
     * */
    private Long clearedSeq;

    /**
     * 删除会话时的消息序号（小于等于该序号时该会话从列表中隐藏，收到新消息后恢复）
     * */
    private Long deletedSeq;
}
