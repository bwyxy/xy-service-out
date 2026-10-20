package com.xy.system.service;

import com.xy.common.vo.chat.ChatSessionVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 客户端用户登录会话服务接口
 *
 * <p>数据来源为 fg-chat 写入 Redis 的登录会话（{@code login:session:{token}}、
 * {@code login:user:{uid}}），因此在线状态以「token 未失效 + 会话状态 ACTIVE」为准。</p>
 */
public interface IChatSessionService {

    /**
     * 查询用户当前全部活跃会话（登录设备）
     *
     * @param uid 用户 UID
     * @return 活跃会话列表
     */
    List<ChatSessionVO> listActiveSessions(Long uid);

    /**
     * 判断用户是否在线
     *
     * @param uid 用户 UID
     * @return true 表示存在活跃会话
     */
    boolean isOnline(Long uid);

    /**
     * 批量统计用户在线设备数
     *
     * @param uids 用户 UID 集合
     * @return uid → 在线设备数（离线用户值为 0）
     */
    Map<Long, Integer> countOnlineSessions(Collection<Long> uids);

    /**
     * 强制下线指定会话
     *
     * @param token  会话令牌
     * @param reason 下线原因（推送给客户端展示）
     * @return true 表示成功处理
     */
    boolean forceLogout(String token, String reason);

    /**
     * 强制下线用户全部会话
     *
     * @param uid    用户 UID
     * @param reason 下线原因
     * @return 被下线的会话数量
     */
    int forceLogoutAll(Long uid, String reason);
}
