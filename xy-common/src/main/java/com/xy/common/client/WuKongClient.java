package com.xy.common.client;

import com.xy.common.client.dto.request.WKUserTokenRequest;
import com.xy.common.client.dto.response.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "wukongClient", url = "${xy.wk-api-url}")
public interface WuKongClient {

    /**
     * 更新用户 token
     */
    @PostMapping("/user/token")
    ApiResult resetUserToken(WKUserTokenRequest wkUserTokenRequest);
}
