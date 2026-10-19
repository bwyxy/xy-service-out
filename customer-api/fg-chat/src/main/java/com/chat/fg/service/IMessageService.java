package com.chat.fg.service;

import com.chat.fg.pojo.dto.MsgDTO;
import com.chat.fg.pojo.dto.RecallMessageDTO;
import com.chat.fg.pojo.dto.SendMsgDTO;
import com.xy.common.client.vo.MessageVO;

/**
 * 频道消息 逻辑层
 * */
public interface IMessageService {

    /**
     * 查询频道历史消息
     * */
    public MessageVO getMessageSync(MsgDTO msgDTO);

    /**
     * 发送消息（经服务端转发至悟空 IM，便于统一校验）
     * */
    public Object sendMessage(SendMsgDTO sendMsgDTO);

    /**
     * 撤回消息
     *
     * <p>校验通过后向频道广播撤回信令，客户端据此替换原消息为「消息已撤回」。</p>
     * */
    public void recallMessage(RecallMessageDTO recallMessageDTO);
}
