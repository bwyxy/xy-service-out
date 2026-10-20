package com.chat.fg.service;

import java.util.List;

/**
 * LiveKit SFU 管理 逻辑层
 *
 * <p>封装服务端 RoomService 调用与接入 token 签发，
 * 供通话逻辑层建房、下发 token、查询/关闭房间使用。</p>
 * */
public interface LivekitAdminService {

    /**
     * 显式创建房间（已存在时 LiveKit 直接返回已有房间，幂等）
     *
     * @param roomName 房间名（与 t_rtc_room.room_id 一致）
     * @param maxParticipants 房间人数上限
     * */
    void ensureRoom(String roomName, int maxParticipants);

    /**
     * 签发接入 token
     *
     * @param roomName 房间名
     * @param uid 用户 ID（作为 LiveKit identity）
     * @param name 展示昵称（群内优先群昵称）
     * @param metadata 附加信息 JSON（uid/nickname/avatar），供前端兜底展示
     * @param roomAdmin 是否为房间管理员（可管理他人）
     * */
    String issueToken(String roomName, Long uid, String name, String metadata, boolean roomAdmin);

    /**
     * 查询房间内在场参与者 identity 列表（房间不存在时返回空列表）
     * */
    List<String> listParticipantIdentities(String roomName);

    /**
     * 关闭房间（踢出全部参与者），房间不存在时静默返回
     * */
    void closeRoom(String roomName);

    /**
     * LiveKit 是否可用（用于健康检查）
     * */
    boolean isUp();
}
