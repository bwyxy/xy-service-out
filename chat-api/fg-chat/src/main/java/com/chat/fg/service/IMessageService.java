package com.chat.fg.service;

import com.chat.fg.pojo.dto.MsgDTO;
import com.xy.common.client.vo.MessageVO;

/**
 * 频道历史消息 逻辑层
 * */
public interface IMessageService {

    /**
     * 查询频道历史消息
     * */
    public MessageVO getMessageSync(MsgDTO msgDTO);
}
