package com.xy.common.client;

import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.client.dto.request.WKUserTokenRequest;
import com.xy.common.client.dto.response.ApiResult;
import com.xy.common.client.dto.response.IMAddrResponse;
import com.xy.common.client.dto.response.MessageSendResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(name = "wukongClient", url = "${xy.wk-api-url}")
public interface WuKongClient {

    /**
     * 更新用户 token
     */
    @PostMapping("/user/token")
    ApiResult<?> resetUserToken(WKUserTokenRequest wkUserTokenRequest);

    /**
     * 获取用户 IM地址
     * */
    @GetMapping("/route")
    IMAddrResponse getRoute(@RequestParam Long userId);

    /**
     * 发送消息
     * */
    @PostMapping("/message/send")
    ApiResult<?> sendMessage(MessageSendRequest messageSendRequest);

}
