package com.chat.fg.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chat.fg.pojo.dto.MsgDTO;
import com.chat.fg.pojo.dto.SendMsgDTO;
import com.chat.fg.service.IMessageService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.client.dto.request.MsgRequest;
import com.xy.common.client.vo.MessageVO;
import com.xy.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

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

        log.warn("校验参数-{}", msgRequest);
        try {
            return wuKongClient.messageSync(msgRequest);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    /**
     * 发送消息（经服务端转发至悟空 IM）
     *
     * <p>统一由服务端构造 payload 并调用悟空 IM /message/send 接口，
     * 便于后续做内容安全校验、敏感词过滤、图片鉴黄等业务扩展。</p>
     * */
    public Object sendMessage(SendMsgDTO sendMsgDTO) {
        Long uId = SecurityUtils.getUId();

        // 1. 构造 payload：{ type, content }
        JSONObject payload = new JSONObject();
        payload.put("type", sendMsgDTO.getType());
        payload.put("content", sendMsgDTO.getContent());
        String payloadBase64 = Base64.getEncoder().encodeToString(payload.toJSONString().getBytes());

        // 2. 构造悟空 IM 发送请求
        MessageSendRequest request = new MessageSendRequest();
        request.setFromUid(String.valueOf(uId));
        request.setChannelId(sendMsgDTO.getChannelId());
        request.setChannelType(sendMsgDTO.getChannelType());
        request.setPayload(payloadBase64);
        request.setClientMsgNo(sendMsgDTO.getClientMsgNo());
        if (request.getClientMsgNo() == null || request.getClientMsgNo().isEmpty()) {
            request.setClientMsgNo(UUID.randomUUID().toString().replace("-", ""));
        }

        // 默认持久化、显示红点
        MessageSendRequest.MessageHeader header = new MessageSendRequest.MessageHeader();
        header.setNoPersist(0);
        header.setRedDot(1);
        request.setHeader(header);

        log.info("发送消息到悟空 IM, fromUid={}, channelId={}, type={}",
                uId, sendMsgDTO.getChannelId(), sendMsgDTO.getType());

        try {
            return wuKongClient.sendMessage(request);
        } catch (Exception e) {
            log.error("发送消息到悟空 IM 失败", e);
            throw new BusinessException("发送消息失败：" + e.getMessage());
        }
    }
}
