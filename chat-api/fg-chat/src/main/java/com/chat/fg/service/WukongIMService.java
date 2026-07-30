package com.chat.fg.service;

import com.chat.fg.config.WukongIMConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.WKUserTokenRequest;
import com.xy.common.client.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 悟空IM HTTP API 客户端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WukongIMService {

    private final WukongIMConfig.WukongIMProperties props;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    private WuKongClient wkKongClient;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /** 更新 token */
    public boolean updateToken(WKUserTokenRequest wkUserTokenRequest) {
        ResultVO result = wkKongClient.resetUserToken(wkUserTokenRequest);

        if (result.getStatus() != 200) {
            throw new SecurityException(result.getMsg());
        }
        return true;
    }

    /** 创建/注册用户，返回 { uid, token, wsUrl } */
    public JsonNode createUser(String uid, String name) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("uid", uid);
        body.put("name", name);

        JsonNode result = post("/user", body);
        ObjectNode response = objectMapper.createObjectNode();

        // log.info("response:{}", response);
        response.put("uid", uid);
        response.put("token", result.get("token").asText());
        response.put("wsUrl", props.getWsUrl());
        return response;
    }

    /** 获取用户 Token */
    public String getUserToken(String uid) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("uid", uid);
        body.put("device_flag", 1); // 与 JS SDK 默认 device_flag=1 保持一致

        JsonNode result = post("/user/token", body);
        return result.has("token") ? result.get("token").asText() : null;
    }

    /** 创建频道 */
    public JsonNode createChannel(String channelId, int channelType) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("channel_id", channelId);
        body.put("channel_type", channelType);

        return post("/channel", body);
    }

    /** 添加频道订阅者 */
    public JsonNode addSubscribers(String channelId, int channelType, String[] uids) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("channel_id", channelId);
        body.put("channel_type", channelType);
        body.putArray("subscribers");
        for (String uid : uids) {
            ((ObjectNode) body).withArray("subscribers").add(uid);
        }

        return post("/channel/subscriber_add", body);
    }

    /** 同步频道消息 */
    public JsonNode syncMessages(String uid, String channelId, int channelType,
                                  long startSeq, long endSeq, int limit) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("login_uid", uid);
        body.put("channel_id", channelId);
        body.put("channel_type", channelType);
        body.put("start_message_seq", startSeq);
        body.put("end_message_seq", endSeq);
        body.put("limit", limit);

        return post("/channel/messagesync", body);
    }

    private JsonNode post(String path, ObjectNode body) throws IOException {
        String url = props.getApiUrl() + path;
        String json = objectMapper.writeValueAsString(body);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, JSON));

        if (props.getManagerToken() != null && !props.getManagerToken().isEmpty()) {
            builder.header("token", props.getManagerToken());
        }

        Request request = builder.build();
        log.debug("WukongIM API: POST {} body={}", url, json);

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "{}";
            log.debug("WukongIM API response: {}", respBody);
            return objectMapper.readTree(respBody);
        }
    }
}