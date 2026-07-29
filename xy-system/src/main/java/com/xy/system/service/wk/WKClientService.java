package com.xy.system.service.wk;

import com.alibaba.fastjson.JSON;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class WKClientService {

    @Autowired
    private WuKongClient wuKongClient;

    /**
     * 发送系统消息
     * @param uId 收信方 id
     * @param content 消息内容
     * */
    public void sendSystemMessage(Long uId, String content) {

        MessageSendRequest messageSendRequest = new MessageSendRequest();

        messageSendRequest.setFromUid("system_admin"); // 使用系统账号发送
        messageSendRequest.setChannelId(String.valueOf(uId)); // 接收方
        messageSendRequest.setChannelType(1);

        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("content", content);
        String jsonStr =  JSON.toJSONString(map);

        String base64Payload = Base64.getEncoder().encodeToString(jsonStr.getBytes(StandardCharsets.UTF_8));

        messageSendRequest.setPayload(base64Payload);

        MessageSendRequest.MessageHeader messageHeader = new MessageSendRequest.MessageHeader();

        messageHeader.setRedDot(1);

        messageSendRequest.setHeader(messageHeader);

        wuKongClient.sendMessage(messageSendRequest);
    }

}
