package com.chat.fg.controller;

import com.chat.fg.service.WukongIMService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.common.annotation.Anonymous;
import com.xy.common.client.dto.request.WKUserTokenRequest;
import com.xy.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 聊天 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final WukongIMService wkService;
    private final ObjectMapper objectMapper;

    public ChatController(WukongIMService wkService,
                          @Qualifier("wukongObjectMapper") ObjectMapper objectMapper) {
        this.wkService = wkService;
        this.objectMapper = objectMapper;
    }

    /** 更新悟空 token */
    @Anonymous
    @PostMapping("/update/token")
    public R<?> updateToken(@RequestBody WKUserTokenRequest wkUserTokenRequest) {

        boolean isSuccess = wkService.updateToken(wkUserTokenRequest);

        if (isSuccess) {
            return R.ok("更新token成功");
        } else {
            return R.error(404,"error");
        }
    }

    /** 用户登录/注册：获取悟空IM连接凭据 */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) throws Exception {
        String uid = body.get("uid");
        String name = body.getOrDefault("name", uid);

        var result = wkService.createUser(uid, name);
        return Map.of(
                "code", 200,
                "data", Map.of(
                        "uid", uid,
                        "token", result.get("token").asText(),
                        "wsUrl", result.get("wsUrl").asText()
                )
        );
    }

    /** 获取用户 Token */
    @PostMapping("/token")
    public Map<String, Object> getToken(@RequestBody Map<String, String> body) throws Exception {
        String uid = body.get("uid");
        String token = wkService.getUserToken(uid);
        return Map.of("code", 200, "data", Map.of("uid", uid, "token", token));
    }

    /** 创建群聊 */
    @PostMapping("/group/create")
    public Map<String, Object> createGroup(@RequestBody Map<String, Object> body) throws Exception {
        String groupId = (String) body.get("groupId");
        String groupName = (String) body.getOrDefault("groupName", groupId);

        // 创建频道
        wkService.createChannel(groupId, 2);

        return Map.of("code", 200, "data", Map.of(
                "groupId", groupId,
                "groupName", groupName
        ));
    }

    /** 加入群聊 */
    @PostMapping("/group/join")
    public Map<String, Object> joinGroup(@RequestBody Map<String, Object> body) throws Exception {
        String groupId = (String) body.get("groupId");
        String uid = (String) body.get("uid");

        wkService.addSubscribers(groupId, 2, new String[]{uid});

        return Map.of("code", 200, "data", Map.of("groupId", groupId, "uid", uid));
    }

    /** 同步消息 */
    @PostMapping("/messages/sync")
    public Map<String, Object> syncMessages(@RequestBody Map<String, Object> body) throws Exception {
        String uid = (String) body.get("uid");
        String channelId = (String) body.get("channelId");
        int channelType = (int) body.getOrDefault("channelType", 1);
        long startSeq = ((Number) body.getOrDefault("startSeq", 0)).longValue();
        long endSeq = ((Number) body.getOrDefault("endSeq", 0)).longValue();
        int limit = (int) body.getOrDefault("limit", 50);

        var result = wkService.syncMessages(uid, channelId, channelType, startSeq, endSeq, limit);
        return Map.of("code", 200, "data", objectMapper.treeToValue(result, Object.class));
    }
}