package com.xy.common.client;

import com.xy.common.client.dto.request.WKUserTokenRequest;
import com.xy.common.client.dto.response.ApiResult;
import com.xy.common.client.dto.response.IMAddrResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "wukongClient", url = "${xy.wk-api-url}")
public interface WuKongClient {

    /**
     * 更新用户 token
     */
    @PostMapping("/user/token")
    ApiResult resetUserToken(WKUserTokenRequest wkUserTokenRequest);

    /**
     * 获取用户 IM地址
     * */
    @GetMapping("/route")
    IMAddrResponse getRoute(@RequestParam Long userId);
}
