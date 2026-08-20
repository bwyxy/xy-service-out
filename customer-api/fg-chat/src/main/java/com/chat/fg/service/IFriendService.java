package com.chat.fg.service;

import com.chat.fg.pojo.dto.FriendRequestDTO;
import com.chat.fg.pojo.dto.HandleFriendRequestDTO;
import com.chat.fg.pojo.vo.FriendRequestVO;
import com.chat.fg.pojo.vo.FriendVO;
import com.xy.common.entity.FriendRequest;
import com.xy.common.entity.User;

import java.util.List;

/**
 * 好友 逻辑层
 * */
public interface IFriendService {

    /**
     * 搜索用户（按昵称、手机号、邮箱模糊匹配）
     * */
    List<User> searchUsers(Long currentUid, String keyword);

    /**
     * 发送好友请求
     * */
    FriendRequest sendFriendRequest(Long fromUid, FriendRequestDTO dto);

    /**
     * 处理好友请求（同意/拒绝）
     * */
    void handleFriendRequest(Long toUid, HandleFriendRequestDTO dto);

    /**
     * 查询收到的请求列表
     * */
    List<FriendRequestVO> listReceivedRequests(Long toUid, Integer status);

    /**
     * 查询发送的请求列表
     * */
    List<FriendRequestVO> listSentRequests(Long fromUid, Integer status);

    /**
     * 查询我的好友列表
     * */
    List<FriendVO> listFriends(Long uId);
}
