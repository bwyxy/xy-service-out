package com.xy.common.ip;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xy.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 获取地址类
 * */
public class AddressUtils {

    private static final Logger log = LoggerFactory.getLogger(AddressUtils.class);

    /**
     * IP地址查询
     * <p>必须走 https：pconline 的 http 入口现已返回 403，读取响应流会抛 IOException，
     * 最终所有记录都落到 UNKNOWN，表现为登录地址统一显示为"未知"。</p>
     * */
    public static final String IP_URL = "https://whois.pconline.com.cn/ipJson.jsp";

    // 未知地址
    public static final String UNKNOWN = "未知";

    // 对方接口固定返回 GBK 编码
    private static final Charset CHARSET_GBK = Charset.forName("GBK");

    // 外部接口可能长时间无响应，必须设置超时，避免异步线程被挂住
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public static String getRealAddressByIP(String ip) {
        // 内网不查询
        if (IpUtils.internalIp(ip)) {
            return "内网IP";
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(IP_URL + "?ip=" + ip + "&json=true"))
                    .timeout(TIMEOUT)
                    .header("accept", "*/*")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();
            String rspStr = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(CHARSET_GBK)).body();
            if (StringUtils.isEmpty(rspStr)) {
                return UNKNOWN;
            }

            JSONObject obj = JSON.parseObject(rspStr);
            String region = obj.getString("pro");
            String city = obj.getString("city");
            List<String> parts = new ArrayList<>(2);
            if (StringUtils.isNotBlank(region)) {
                parts.add(region.trim());
            }
            // 直辖市等场景 pro 与 city 相同，去重避免出现"北京市 北京市"
            if (StringUtils.isNotBlank(city) && !city.trim().equals(StringUtils.trim(region))) {
                parts.add(city.trim());
            }
            if (!parts.isEmpty()) {
                return String.join(" ", parts);
            }

            // 境外 IP 没有省市，只有 addr（如"美国"）
            String addr = obj.getString("addr");
            return StringUtils.isNotBlank(addr) ? addr.trim() : UNKNOWN;
        } catch (Exception e) {
            log.error("获取地理位置异常 {}", ip, e);
        }
        return UNKNOWN;
    }
}
