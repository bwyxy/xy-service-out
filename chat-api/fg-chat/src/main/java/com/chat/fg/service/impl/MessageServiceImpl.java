package com.chat.fg.service.impl;

import com.alibaba.fastjson.JSON;
import com.chat.fg.pojo.dto.MsgDTO;
import com.chat.fg.service.IMessageService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MsgRequest;
import com.xy.common.client.vo.MessageVO;
import com.xy.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessageServiceImpl implements IMessageService {

    @Autowired
    private WuKongClient wuKongClient;

    /**
     * 查询频道历史消息
     * */
    public MessageVO getMessageSync(MsgDTO msgDTO) {
        Long uId = SecurityUtils.getUId();

        MsgRequest msgRequest = new MsgRequest();

        if (msgDTO.getLimit() == 0) {
            msgRequest.setLimit(30);
        } else {
            msgRequest.setLimit(msgDTO.getLimit());
        }

        msgRequest.setLoginUid(String.valueOf(uId));
        msgRequest.setChannelId(msgDTO.getChannelId());
        msgRequest.setChannelType(msgDTO.getChannelType());
        msgRequest.setPullMode(msgDTO.getPullMode());
        msgRequest.setStartMessageSeq(msgDTO.getStartMessageSeq());
        msgRequest.setEndMessageSeq(msgDTO.getEndMessageSeq());

        try {
            return wuKongClient.messageSync(msgRequest);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }
}
