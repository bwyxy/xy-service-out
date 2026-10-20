package com.chat.fg.service.impl;

import com.chat.fg.config.RtcProperties;
import com.chat.fg.service.LivekitAdminService;
import com.xy.common.exception.BusinessException;
import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomAdmin;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * LiveKit SFU 管理 逻辑实现
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class LivekitAdminServiceImpl implements LivekitAdminService {

    private final RtcProperties rtcProperties;

    /**
     * RoomServiceClient 单例（内部持有 OkHttp 连接池，重复创建会泄漏连接）
     * */
    private volatile RoomServiceClient client;

    @Override
    public void ensureRoom(String roomName, int maxParticipants) {
        RtcProperties.Livekit cfg = rtcProperties.getLivekit();
        try {
            Response<LivekitModels.Room> response = client().createRoom(
                    roomName,
                    cfg.getEmptyTimeoutSeconds(),
                    maxParticipants,
                    null,
                    null,
                    null,
                    null,
                    null,
                    cfg.getDepartureTimeoutSeconds()
            ).execute();
            if (!response.isSuccessful()) {
                throw new BusinessException("LiveKit 房间创建失败，code=" + response.code());
            }
        } catch (IOException e) {
            throw new BusinessException("LiveKit 服务不可达，请检查 SFU 部署");
        }
    }

    @Override
    public String issueToken(String roomName, Long uid, String name, String metadata, boolean roomAdmin) {
        RtcProperties.Livekit cfg = rtcProperties.getLivekit();

        AccessToken token = new AccessToken(cfg.getApiKey(), cfg.getApiSecret());
        token.setIdentity(String.valueOf(uid));
        token.setName(name);
        token.setMetadata(metadata);
        // setTtl 单位为毫秒
        token.setTtl(cfg.getTokenTtlSeconds() * 1000);
        token.addGrants(
                new RoomJoin(true),
                new RoomName(roomName),
                new CanPublish(true),
                new CanSubscribe(true)
        );
        if (roomAdmin) {
            token.addGrants(new RoomAdmin(true));
        }
        return token.toJwt();
    }

    @Override
    public List<String> listParticipantIdentities(String roomName) {
        try {
            Response<List<LivekitModels.ParticipantInfo>> response = client().listParticipants(roomName).execute();
            if (!response.isSuccessful() || response.body() == null) {
                // 房间不存在时 LiveKit 返回 404，视为无人在场
                return Collections.emptyList();
            }
            return response.body().stream()
                    .map(LivekitModels.ParticipantInfo::getIdentity)
                    .toList();
        } catch (IOException e) {
            throw new BusinessException("查询 LiveKit 房间参与者失败");
        }
    }

    @Override
    public void closeRoom(String roomName) {
        try {
            Response<Void> response = client().deleteRoom(roomName).execute();
            if (!response.isSuccessful()) {
                log.warn("关闭 LiveKit 房间失败, roomName={}, code={}", roomName, response.code());
            }
        } catch (IOException e) {
            log.warn("关闭 LiveKit 房间异常, roomName={}", roomName, e);
        }
    }

    @Override
    public boolean isUp() {
        try {
            return client().listRooms().execute().isSuccessful();
        } catch (Exception e) {
            log.warn("LiveKit 健康检查失败", e);
            return false;
        }
    }

    /**
     * 懒加载单例，避免启动时 LiveKit 不可达导致应用启动失败
     * */
    private RoomServiceClient client() {
        RoomServiceClient local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    RtcProperties.Livekit cfg = rtcProperties.getLivekit();
                    local = RoomServiceClient.Companion.createClient(cfg.getApiUrl(), cfg.getApiKey(), cfg.getApiSecret());
                    client = local;
                }
            }
        }
        return local;
    }
}
