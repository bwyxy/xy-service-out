package com.xy.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.enums.ChannelType;
import com.xy.common.enums.MessageType;
import com.xy.common.enums.OfficialAccount;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.common.utils.StringUtils;
import com.xy.common.vo.chat.ChatSessionVO;
import com.xy.common.web.service.TokenBlacklistService;
import com.xy.system.service.IChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 客户端用户登录会话服务实现
 *
 * <p>直接读写 fg-chat 落库的 Redis 会话，存储结构对齐 {@code LoginSessionServiceImpl}：</p>
 * <ul>
 *   <li>会话明细：{@code login:session:{token}} → JSON 字符串</li>
 *   <li>用户维度索引：{@code login:user:{uid}} → SET&lt;token&gt;</li>
 *   <li>设备类型索引：{@code login:user:{uid}:type:{deviceType}} → SET&lt;token&gt;</li>
 * </ul>
 * <p>注意：fg-chat 使用 StringRedisTemplate + fastjson 写入，此处必须保持完全一致，
 * 不可改用 xy-system 的 RedisCache（JDK 序列化）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements IChatSessionService {

    private static final String SESSION_KEY_PREFIX = "login:session:";
    private static final String USER_KEY_PREFIX = "login:user:";

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_KICKED = "KICKED";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate redisTemplate;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final WuKongClient wuKongClient;

    @Override
    public List<ChatSessionVO> listActiveSessions(Long uid) {
        if (uid == null) {
            return Collections.emptyList();
        }
        List<String> tokenList = new ArrayList<>(indexTokens(uid));
        if (tokenList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> jsons = redisTemplate.opsForValue().multiGet(sessionKeys(tokenList));
        List<ChatSessionVO> sessions = new ArrayList<>();
        List<String> staleTokens = new ArrayList<>();

        for (int i = 0; i < tokenList.size(); i++) {
            String json = jsons == null ? null : jsons.get(i);
            if (StringUtils.isEmpty(json)) {
                // 明细已随 TTL 过期，清理索引残留
                staleTokens.add(tokenList.get(i));
                continue;
            }
            JSONObject session = parseSession(json, tokenList.get(i));
            if (session == null) {
                staleTokens.add(tokenList.get(i));
                continue;
            }
            if (STATUS_ACTIVE.equals(session.getString("status"))) {
                sessions.add(toVO(session, tokenList.get(i)));
            }
        }

        removeStaleIndex(uid, staleTokens);
        return sessions;
    }

    @Override
    public boolean isOnline(Long uid) {
        return !listActiveSessions(uid).isEmpty();
    }

    @Override
    public Map<Long, Integer> countOnlineSessions(Collection<Long> uids) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        if (uids == null || uids.isEmpty()) {
            return result;
        }

        for (Long uid : uids) {
            if (uid == null) {
                continue;
            }
            List<String> tokenList = new ArrayList<>(indexTokens(uid));
            if (tokenList.isEmpty()) {
                result.put(uid, 0);
                continue;
            }

            List<String> jsons = redisTemplate.opsForValue().multiGet(sessionKeys(tokenList));
            int active = 0;
            List<String> staleTokens = new ArrayList<>();
            for (int i = 0; i < tokenList.size(); i++) {
                String json = jsons == null ? null : jsons.get(i);
                if (StringUtils.isEmpty(json)) {
                    staleTokens.add(tokenList.get(i));
                    continue;
                }
                JSONObject session = parseSession(json, tokenList.get(i));
                if (session != null && STATUS_ACTIVE.equals(session.getString("status"))) {
                    active++;
                }
            }
            removeStaleIndex(uid, staleTokens);
            result.put(uid, active);
        }
        return result;
    }

    @Override
    public boolean forceLogout(String token, String reason) {
        if (StringUtils.isEmpty(token)) {
            return false;
        }

        JSONObject session = readSession(token);
        long remaining = remainingTtl(token);

        if (session == null && remaining <= 0) {
            return false;
        }

        Long uid = null;
        Integer deviceType = null;
        String deviceName = null;

        if (session != null) {
            uid = session.getLong("uid");
            deviceType = session.getInteger("deviceType");
            deviceName = session.getString("deviceName");

            session.put("status", STATUS_KICKED);
            if (remaining > 0) {
                // 保留失效记录便于追溯，TTL 与 token 剩余有效期对齐
                redisTemplate.opsForValue().set(
                        SESSION_KEY_PREFIX + token,
                        session.toJSONString(),
                        remaining,
                        TimeUnit.MILLISECONDS
                );
            } else {
                redisTemplate.delete(SESSION_KEY_PREFIX + token);
            }

            if (uid != null) {
                removeFromIndex(uid, deviceType, token);
            }
        }

        if (remaining > 0) {
            try {
                tokenBlacklistService.blacklist(token, remaining);
            } catch (Exception e) {
                log.warn("拉黑会话 token 失败, token={}", token, e);
            }
        }

        if (uid != null) {
            notifyForceLogout(uid, deviceType, deviceName, token, reason);
        }
        return true;
    }

    @Override
    public int forceLogoutAll(Long uid, String reason) {
        if (uid == null) {
            return 0;
        }
        int count = 0;
        for (String token : indexTokens(uid)) {
            if (forceLogout(token, reason)) {
                count++;
            }
        }
        return count;
    }

    // ==================== 内部方法 ====================

    private Set<String> indexTokens(Long uid) {
        Set<String> tokens = redisTemplate.opsForSet().members(USER_KEY_PREFIX + uid);
        return tokens == null ? Collections.emptySet() : tokens;
    }

    private List<String> sessionKeys(List<String> tokens) {
        List<String> keys = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            keys.add(SESSION_KEY_PREFIX + token);
        }
        return keys;
    }

    private JSONObject readSession(String token) {
        String json = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + token);
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        return parseSession(json, token);
    }

    private JSONObject parseSession(String json, String token) {
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            log.warn("解析登录会话失败, token={}", token, e);
            return null;
        }
    }

    private void removeStaleIndex(Long uid, List<String> staleTokens) {
        if (staleTokens.isEmpty()) {
            return;
        }
        redisTemplate.opsForSet().remove(USER_KEY_PREFIX + uid, staleTokens.toArray());
    }

    private void removeFromIndex(Long uid, Integer deviceType, String token) {
        redisTemplate.opsForSet().remove(USER_KEY_PREFIX + uid, token);
        if (deviceType != null) {
            redisTemplate.opsForSet().remove(USER_KEY_PREFIX + uid + ":type:" + deviceType, token);
        }
    }

    private long remainingTtl(String token) {
        try {
            return jwtTokenProvider.getRemainingExpiration(token);
        } catch (Exception e) {
            log.warn("解析 token 剩余有效期失败, token={}", token, e);
            return 0L;
        }
    }

    private ChatSessionVO toVO(JSONObject session, String token) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setToken(StringUtils.isNotEmpty(session.getString("token")) ? session.getString("token") : token);
        Integer deviceType = session.getInteger("deviceType");
        vo.setDeviceType(deviceType);
        vo.setDeviceTypeLabel(deviceTypeLabel(deviceType));
        vo.setDeviceName(session.getString("deviceName"));
        vo.setDeviceId(session.getString("deviceId"));
        vo.setIp(session.getString("ip"));
        vo.setLoginTime(formatTime(session.getLong("loginTime")));
        vo.setLastActiveTime(formatTime(session.getLong("lastActiveTime")));
        return vo;
    }

    private String deviceTypeLabel(Integer deviceType) {
        if (deviceType == null) {
            return "未知设备";
        }
        return switch (deviceType) {
            case 1 -> "PC端";
            case 2 -> "平板端";
            case 3 -> "手机端";
            default -> "未知设备";
        };
    }

    private String formatTime(Long millis) {
        if (millis == null || millis <= 0) {
            return null;
        }
        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .format(TIME_FORMATTER);
    }

    /**
     * 推送强制下线通知（220 信令）
     * <p>与 fg-chat 的 ForceLogoutNotifier 保持一致，客户端比对 kickedToken 后清除本地登录态。</p>
     */
    private void notifyForceLogout(Long uid, Integer deviceType, String deviceName, String token, String reason) {
        try {
            JSONObject content = new JSONObject();
            content.put("uid", uid);
            content.put("deviceType", deviceType);
            content.put("deviceName", deviceName);
            content.put("kickedToken", token);
            content.put("reason", reason);

            JSONObject payload = new JSONObject();
            payload.put("type", MessageType.forceLogout.getCode());
            payload.put("content", content.toJSONString());

            String payloadBase64 = Base64.getEncoder()
                    .encodeToString(payload.toJSONString().getBytes(StandardCharsets.UTF_8));

            MessageSendRequest request = new MessageSendRequest();
            request.setFromUid(OfficialAccount.systemAdmin.getCode());
            request.setChannelId(String.valueOf(uid));
            request.setChannelType(ChannelType.PERSONAL_CHANNEL.getCode());
            request.setPayload(payloadBase64);
            request.setClientMsgNo(UUID.randomUUID().toString().replace("-", ""));

            MessageSendRequest.MessageHeader header = new MessageSendRequest.MessageHeader();
            header.setNoPersist(1);
            header.setRedDot(0);
            header.setSyncOnce(0);
            request.setHeader(header);

            wuKongClient.sendMessage(request);
        } catch (Exception e) {
            // 推送失败不阻断强退：token 黑名单已生效，客户端下次请求会被拦截
            log.error("推送强制下线通知失败, uid={}, deviceType={}", uid, deviceType, e);
        }
    }
}
