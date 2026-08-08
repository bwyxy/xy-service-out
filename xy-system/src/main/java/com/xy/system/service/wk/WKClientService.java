package com.xy.system.service.wk;

import com.alibaba.fastjson.JSON;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.enums.ChannelType;
import com.xy.common.enums.OfficialAccount;
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
     * 发送系统官方消息
     * @param uId 收信方 id
     * @param content 消息内容
     * */
    public void sendSystemMessage(Long uId, String content) {

        MessageSendRequest messageSendRequest = new MessageSendRequest();
        String systemAdmin = OfficialAccount.systemAdmin.getCode();

        messageSendRequest.setFromUid(systemAdmin); // 使用系统账号发送
        sendMessage(uId, content, messageSendRequest);
    }

    /**
     * 发送系统通知
     *  @param uId 收信方 id
     *  @param content 消息内容
     * */
    public void sendSystemNotice(Long uId, String content) {
        MessageSendRequest messageSendRequest = new MessageSendRequest();
        String systemNotice = OfficialAccount.systemNotice.getCode();
        // 设置为系统通知账号
        messageSendRequest.setFromUid(systemNotice);
        sendMessage(uId, content, messageSendRequest);
    }

    private void sendMessage(Long uId, String content, MessageSendRequest messageSendRequest) {
        messageSendRequest.setChannelId(String.valueOf(uId)); // 接收方
        messageSendRequest.setChannelType(ChannelType.PERSONAL_CHANNEL.getCode());

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
