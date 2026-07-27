package com.chat.fg.utils;

import com.alibaba.fastjson2.JSON;
import com.chat.fg.config.WukongIMConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xy.common.utils.http.HttpUtils;

/**
 * 悟空IM Utils类
 * */
public class WuKongUtils {

    private WukongIMConfig.WukongIMProperties props;
    /** 更新用户 token
     * token 使用用户登录时的token
     * */
    public boolean resetWKToken(Long uid, String token, int deviceFlag, int deviceLevel) {
        ObjectNode body = new ObjectMapper().createObjectNode();

        body.put("uid", uid);
        body.put("token", token);
        body.put("device_flag", deviceFlag);
        body.put("device_level", deviceLevel);

        String url = props.getApiUrl() + "/user/token";
        String response = HttpUtils.sendPost(url, body.toString());


        return true;
    }

}
