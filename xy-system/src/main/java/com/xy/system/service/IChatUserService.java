package com.xy.system.service;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.User;
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

    /**
     * 按 UID 查询客户端用户实体
     *
     * <p>供各客户端应用解析登录态使用（如待办小程序的 todo_{uId}）。</p>
     *
     * @param uId 用户 UID
     * @return 用户实体，不存在返回 null
     */
    User getByUId(Long uId);

    /**
     * 按微信 openid 查询客户端用户
     *
     * @param openId 微信 openid
     * @return 用户实体，不存在返回 null
     */
    User getByOpenId(String openId);

    /**
     * 按 openid 注册客户端用户
     *
     * <p>微信小程序用户没有手机号与密码，建号时 password 写空串占位（列为 NOT NULL），
     * phone/email 留 null（列上有唯一索引且默认空串，多个空串会互相冲突），
     * is_bind_chat 置为未绑定。</p>
     *
     * @param openId 微信 openid
     * @return 新建或并发场景下已存在的用户
     */
    User registerByOpenId(String openId);
}
