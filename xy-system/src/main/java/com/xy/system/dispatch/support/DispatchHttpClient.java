package com.xy.system.dispatch.support;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.system.dispatch.DispatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 分发模块统一的第三方平台 HTTP 客户端
 * 负责超时控制与网络异常归一化，业务语义（重试、限流、错误映射）由适配器层处理
 * */
@Slf4j
@Component
public class DispatchHttpClient {

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/122.0 Safari/537.36";

    /**
     * 发起请求
     * @param method HTTP 方法
     * @param url 请求地址
     * @param headers 请求头
     * @param body 请求体，可为空
     * @param timeoutSeconds 超时秒数
     * */
    public DispatchHttpResponse execute(String method, String url, Map<String, String> headers,
                                        String body, int timeoutSeconds) {
        long start = System.currentTimeMillis();
        try (HttpResponse response = buildRequest(method, url, headers, body, timeoutSeconds).execute()) {
            long cost = System.currentTimeMillis() - start;
            return new DispatchHttpResponse(response.getStatus(), response.body(), cost);
        } catch (Exception e) {
            log.warn("调用平台接口失败 url={} method={} err={}", url, method, e.getMessage());
            throw DispatchException.networkError("调用平台接口失败：" + e.getMessage(), e);
        }
    }

    /**
     * GET 请求
     * */
    public DispatchHttpResponse get(String url, Map<String, String> headers, int timeoutSeconds) {
        return execute("GET", url, headers, null, timeoutSeconds);
    }

    /**
     * POST JSON 请求
     * */
    public DispatchHttpResponse postJson(String url, Map<String, String> headers, String jsonBody, int timeoutSeconds) {
        return execute("POST", url, headers, jsonBody, timeoutSeconds);
    }

    /**
     * POST multipart 文件上传
     * @param fieldName 表单字段名
     * */
    public DispatchHttpResponse postFile(String url, Map<String, String> headers, String fieldName,
                                         byte[] bytes, String fileName, int timeoutSeconds) {
        long start = System.currentTimeMillis();
        try (HttpResponse response = HttpRequest.post(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .headerMap(headers, true)
                .form(fieldName, bytes, fileName)
                .timeout(timeoutSeconds * 1000)
                .execute()) {
            long cost = System.currentTimeMillis() - start;
            return new DispatchHttpResponse(response.getStatus(), response.body(), cost);
        } catch (Exception e) {
            log.warn("上传文件到平台失败 url={} err={}", url, e.getMessage());
            throw DispatchException.networkError("上传文件到平台失败：" + e.getMessage(), e);
        }
    }

    /**
     * 下载远程资源（用于把外链图片迁移到平台图床）
     * */
    public byte[] download(String url, int timeoutSeconds) {
        try (HttpResponse response = HttpRequest.get(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .timeout(timeoutSeconds * 1000)
                .execute()) {
            if (!response.isOk()) {
                throw new DispatchException(DispatchErrorTypeEnum.PLATFORM_ERROR,
                        "下载资源失败，HTTP 状态码 " + response.getStatus());
            }
            return response.bodyBytes();
        } catch (DispatchException e) {
            throw e;
        } catch (Exception e) {
            throw DispatchException.networkError("下载资源失败：" + e.getMessage(), e);
        }
    }

    private HttpRequest buildRequest(String method, String url, Map<String, String> headers,
                                     String body, int timeoutSeconds) {
        HttpRequest request;
        String normalized = method == null ? "GET" : method.toUpperCase();
        switch (normalized) {
            case "POST" -> request = HttpRequest.post(url);
            case "GET" -> request = HttpRequest.get(url);
            default -> request = HttpRequest.of(url).method(cn.hutool.http.Method.valueOf(normalized));
        }
        request.header("User-Agent", DEFAULT_USER_AGENT).timeout(timeoutSeconds * 1000);
        if (headers != null && !headers.isEmpty()) {
            request.headerMap(headers, true);
        }
        if (body != null) {
            request.body(body, "application/json");
        }
        return request;
    }

    /**
     * 安全截断响应体，避免超长内容撑爆日志与错误信息字段
     * */
    public static String abbreviate(String body, int maxLength) {
        if (body == null) {
            return "";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxLength) {
            return body;
        }
        return new String(bytes, 0, maxLength, StandardCharsets.UTF_8) + "...(已截断)";
    }
}
