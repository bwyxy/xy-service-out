package com.xy.system.service;

import com.xy.common.core.domain.PageResult;
import com.xy.common.vo.chat.ChatUserQuery;
import com.xy.common.vo.chat.ChatUserVO;

import java.util.List;

/**
 * 客户端用户（t_user）服务接口
 */
public interface IChatUserService {

    /**
     * 分页查询客户端用户
     *
     * @param query 查询条件（含在线状态过滤）
     * @return 分页结果
     */
    PageResult<ChatUserVO> selectChatUserPage(ChatUserQuery query);

    /**
     * 查询用户详情
     *
     * @param uId 用户 UID
     * @return 用户详情（含在线状态与在线设备数）
     */
    ChatUserVO getChatUserDetail(Long uId);

    /**
     * 修改用户状态
     *
     * <p>禁用时会同步强制下线该用户全部会话，避免已登录设备继续收发消息。</p>
     *
     * @param uId    用户 UID
     * @param status 用户状态 0-正常 1-禁用
     */
    void updateStatus(Long uId, String status);

    /**
     * 按昵称/手机号/邮箱/UID 搜索用户（用于黑名单选择目标用户）
     *
     * @param keyword 关键字
     * @param limit   最大返回条数
     * @return 用户列表
     */
    List<ChatUserVO> searchChatUsers(String keyword, Integer limit);
}
