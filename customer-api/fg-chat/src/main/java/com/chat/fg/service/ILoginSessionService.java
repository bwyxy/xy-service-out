package com.chat.fg.service;

import com.chat.fg.enums.SessionStatus;
import com.chat.fg.pojo.LoginSession;

import java.util.List;

/**
 * 登录会话服务
 *
 * <p>以用户 ID + 设备类型为维度管理登录会话：
 * 同一设备类型下最多保留 1 个有效活跃会话。</p>
 */
public interface ILoginSessionService {

    /**
     * 记录新会话，并踢掉该用户在同一设备类型下的所有旧会话
     *
     * @param session   新会话
     * @param ttlMillis 会话有效期（毫秒，通常取 token 剩余有效期）
     * @return 被强制下线的旧会话列表（用于推送强制下线通知）
     */
    List<LoginSession> replaceSession(LoginSession session, long ttlMillis);

    /**
     * 查询会话明细
     */
    LoginSession getSession(String token);

    /**
     * 查询某用户全部活跃会话
     */
    List<LoginSession> listActiveSessions(Long uid);

    /**
     * 查询某用户指定设备类型下的活跃会话
     */
    List<LoginSession> listActiveSessions(Long uid, Integer deviceType);

    /**
     * 刷新最后活跃时间（内部节流，避免每个请求都写 Redis）
     */
    void touch(String token);

    /**
     * 结束会话：标记状态、移出活跃索引并拉黑 token
     *
     * @param status 结束原因（KICKED=被顶下线 / LOGOUT=主动登出）
     */
    void endSession(String token, SessionStatus status);
}