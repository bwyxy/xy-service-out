package com.chat.fg.wk;

import com.alibaba.fastjson.JSON;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.constant.Constants;
import com.xy.common.enums.ChannelType;
import com.xy.common.enums.MessageType;
import com.xy.common.enums.OfficialAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ChatMessage {

    @Autowired
    private WuKongClient wuKongClient;

    /**
     * 发送公共群聊消息（新成员加入通知）
     * */
    public void sendGroupMessage(String groupId, String msgContent) {
        sendGroupMessage(groupId, MessageType.newMember, msgContent);
    }

    /**
     * 发送公共群聊消息
     *
     * <p>以频道系统消息的形式发送：不携带发送者，群内所有订阅者均可见。</p>
     * */
    public void sendGroupMessage(String groupId, MessageType messageType, String msgContent) {
        MessageSendRequest messageSendRequest = new MessageSendRequest();

        messageSendRequest.setChannelId(Constants.GROUP_ID_PREFIX + groupId);
        messageSendRequest.setChannelType(ChannelType.GROUP_CHANNEL.getCode());

        Map<String, Object> map = new HashMap<>();

        map.put("type", messageType.getCode());
        map.put("content", msgContent);
        String jsonStr = JSON.toJSONString(map);

        String base64Payload = Base64.getEncoder().encodeToString(jsonStr.getBytes(StandardCharsets.UTF_8));

        messageSendRequest.setPayload(base64Payload);

        MessageSendRequest.MessageHeader messageHeader = new MessageSendRequest.MessageHeader();

        messageHeader.setRedDot(1);

        messageSendRequest.setHeader(messageHeader);

        wuKongClient.sendMessage(messageSendRequest);
    }

    /**
     * 向指定用户的个人频道发送系统通知
     *
     * <p>以系统账号身份发送、不持久化、不显示红点，客户端收到后弹出系统通知。</p>
     * */
    public void sendUserNotice(Long toUid, MessageType messageType, String title, String content, Map<String, Object> extra) {
        Map<String, Object> payload = new HashMap<>(4);

        payload.put("type", messageType.getCode());
        payload.put("title", title);
        payload.put("content", content);

        if (extra != null && !extra.isEmpty()) {
            payload.put("extra", extra);
        }

        String base64Payload = Base64.getEncoder()
                .encodeToString(JSON.toJSONString(payload).getBytes(StandardCharsets.UTF_8));

        MessageSendRequest messageSendRequest = new MessageSendRequest();

        messageSendRequest.setFromUid(OfficialAccount.systemAdmin.getCode());
        messageSendRequest.setChannelId(String.valueOf(toUid));
        messageSendRequest.setChannelType(ChannelType.PERSONAL_CHANNEL.getCode());
        messageSendRequest.setPayload(base64Payload);
        messageSendRequest.setClientMsgNo(UUID.randomUUID().toString().replace("-", ""));

        MessageSendRequest.MessageHeader messageHeader = new MessageSendRequest.MessageHeader();

        messageHeader.setNoPersist(1);
        messageHeader.setRedDot(0);

        messageSendRequest.setHeader(messageHeader);

        wuKongClient.sendMessage(messageSendRequest);
    }
}
