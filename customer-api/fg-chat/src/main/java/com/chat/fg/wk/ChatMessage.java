package com.chat.fg.wk;

import com.alibaba.fastjson.JSON;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.constant.Constants;
import com.xy.common.enums.ChannelType;
import com.xy.common.enums.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatMessage {

    @Autowired
    private WuKongClient wuKongClient;

    /**
     * 发送公共群聊消息
     * */
    public void sendGroupMessage(
            String msgId,
            List<String> members,
            String msgContent
    ) {
        MessageSendRequest messageSendRequest = new MessageSendRequest();

        messageSendRequest.setChannelId(Constants.GROUP_ID_PREFIX + msgId);
        messageSendRequest.setChannelType(ChannelType.GROUP_CHANNEL.getCode());
//        messageSendRequest.setSubscribers(members);

        Map<String, Object> map = new HashMap<>();

        map.put("type", MessageType.newMember.getCode());
        map.put("content", msgContent);
        String jsonStr = JSON.toJSONString(map);

        String base64Payload = Base64.getEncoder().encodeToString(jsonStr.getBytes(StandardCharsets.UTF_8));

        messageSendRequest.setPayload(base64Payload);

        MessageSendRequest.MessageHeader messageHeader = new MessageSendRequest.MessageHeader();

        messageHeader.setRedDot(1);

        messageSendRequest.setHeader(messageHeader);

        wuKongClient.sendMessage(messageSendRequest);
    }
}
