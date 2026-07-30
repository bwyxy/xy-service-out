package com.xy.common.client;

import com.xy.common.client.dto.request.*;
import com.xy.common.client.vo.ResultVO;
import com.xy.common.client.vo.ConversationVO;
import com.xy.common.client.dto.response.IMAddrResponse;
import com.xy.common.client.entity.BaseEntity;
import com.xy.common.client.vo.MessageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "wukongClient", url = "${xy.wk-api-url}")
public interface WuKongClient {

    /**
     * 更新用户 token
     */
    @PostMapping("/user/token")
    ResultVO<?> resetUserToken(WKUserTokenRequest wkUserTokenRequest);

    /**
     * 获取用户 IM地址
     * */
    @GetMapping("/route")
    IMAddrResponse getRoute(@RequestParam Long userId);

    /**
     * 发送消息
     * */
    @PostMapping("/message/send")
    ResultVO<?> sendMessage(MessageSendRequest messageSendRequest);

    /**
     * 同步用户会话
     * */
    @PostMapping("/conversation/sync")
    List<ConversationVO> syncConversation(ConversationRequest conversationRequest);

    /**
     * 清除未读消息
     * */
    @PostMapping("/conversations/clearUnread")
    ResultVO<?> clearUnreadConversation(ClearUnread clearUnread);

    /**
     * 设置会话未读数
     * */
    @PostMapping("/conversations/setUnread")
    ResultVO<?> setUnreadConversation(SetUnread setUnread);

    /**
     * 删除会话
     * */
    @PostMapping("/conversations/delete")
    ResultVO<?> delConversation(BaseEntity baseEntity);

    /**
     * 频道历史消息
     * */
    @PostMapping(path = "/channel/messagesync", consumes = "application/json")
    MessageVO messageSync(@RequestBody MsgRequest msgRequest);

    /**
     * 获取系统用户
     * */
    @GetMapping("/user/systemuids")
    List<String> getSystemUids();
}
