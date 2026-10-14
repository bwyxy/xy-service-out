package com.chat.fg.service;

import com.alibaba.fastjson.JSONObject;
import com.chat.fg.pojo.LoginSession;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.enums.ChannelType;
import com.xy.common.enums.MessageType;
import com.xy.common.enums.OfficialAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * 强制下线通知推送
 *
 * <p>会话被同类型设备顶下线后，通过悟空 IM 长连接下发 220 信令，
 * 由客户端弹出提示并清除本地登录态。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForceLogoutNotifier {

    private final WuKongClient wuKongClient;

    /**
     * 推送强制下线通知
     *
     * @param session 被顶下线的旧会话
     */
    public void notifyKicked(LoginSession session) {
        if (session == null || session.getUid() == null) {
            return;
        }

        JSONObject content = new JSONObject();
        content.put("uid", session.getUid());
        content.put("deviceType", session.getDeviceType());
        content.put("deviceName", session.getDeviceName());
        // 携带被踢 token，客户端可比对本地 token，避免误踢新登录设备
        content.put("kickedToken", session.getToken());
        content.put("reason", "您的账号已在其他同类型设备上登录，当前设备已被强制退出");

        JSONObject payload = new JSONObject();
        payload.put("type", MessageType.forceLogout.getCode());
        payload.put("content", content.toJSONString());

        String payloadBase64 = Base64.getEncoder()
                .encodeToString(payload.toJSONString().getBytes(StandardCharsets.UTF_8));

        MessageSendRequest request = new MessageSendRequest();
        request.setFromUid(OfficialAccount.systemAdmin.getCode());
        request.setChannelId(String.valueOf(session.getUid()));
        request.setChannelType(ChannelType.PERSONAL_CHANNEL.getCode());
        request.setPayload(payloadBase64);
        request.setClientMsgNo(UUID.randomUUID().toString().replace("-", ""));

        // 信令类消息：不持久化、不显示红点、写扩散
        MessageSendRequest.MessageHeader header = new MessageSendRequest.MessageHeader();
        header.setNoPersist(1);
        header.setRedDot(0);
        header.setSyncOnce(0);
        request.setHeader(header);

        try {
            wuKongClient.sendMessage(request);
        } catch (Exception e) {
            // 推送失败不阻断登录主流程：离线设备由 token 黑名单兜底
            log.error("推送强制下线通知失败, uid={}, deviceType={}",
                    session.getUid(), session.getDeviceType(), e);
        }
    }
}