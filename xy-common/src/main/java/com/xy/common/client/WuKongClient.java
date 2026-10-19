package com.xy.common.client;

import com.xy.common.client.dto.request.*;
import com.xy.common.client.vo.ResultVO;
import com.xy.common.client.vo.ConversationVO;
import com.xy.common.client.dto.response.IMAddrResponse;
import com.xy.common.client.entity.BaseEntity;
import com.xy.common.client.vo.MessageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping(path = "/channel/messagesync")
    MessageVO messageSync(MsgRequest msgRequest);

    /**
     * 获取系统用户
     * */
    @GetMapping("/user/systemuids")
    List<String> getSystemUids();

    /**
     * 添加系统用户
     * */
    @PostMapping("/user/systemuids_add")
    ResultVO<?> addSystemUids(@RequestBody List<String> systemUids);

    /**
     * 删除系统用户
     * */
    @PostMapping("/user/systemuids_remove")
    ResultVO<?> removeSystemUids(@RequestBody List<String> systemUids);

    /** 添加更新频道 */
    @PostMapping("/channel")
    ResultVO<?> saveChannel(@RequestBody Channel channel);

    /** 删除频道 */
    @PostMapping("/channel/delete")
    ResultVO<?> deleteChannel(@RequestBody Channel channel);

    /** 添加订阅者 */
    @PostMapping("/channel/subscriber_add")
    ResultVO<?> addSubscribers(@RequestBody AddSubscriber addSubscriber);

    /** 移除订阅者 */
    @PostMapping("/channel/subscriber_remove")
    ResultVO<?> removeSubscribers(@RequestBody BaseChannel baseChannel);

    /** 添加黑名单 */
    @PostMapping("/channel/blacklist_add")
    ResultVO<?> addBlackList(@RequestBody BaseChannel baseChannel);

    /** 移除黑名单 */
    @PostMapping("/channel/blacklist_remove")
    ResultVO<?> removeBlackList(@RequestBody BaseChannel baseChannel);

    /** 设置黑名单 */
    @PostMapping("/channel/blacklist_set")
    ResultVO<?> setBlackList(@RequestBody BaseChannel baseChannel);

    /** 添加白名单 */
    @PostMapping("/channel/whitelist_add")
    ResultVO<?> addWhiteList(@RequestBody BaseChannel baseChannel);

    /** 移除白名单 */
    @PostMapping("/channel/whitelist_remove")
    ResultVO<?> removeWhiteList(@RequestBody BaseChannel baseChannel);

    /** 设置白名单 */
    @PostMapping("/channel/whitelist_set")
    ResultVO<?> setWhiteList(@RequestBody BaseChannel baseChannel);
}
