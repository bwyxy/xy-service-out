package com.xy.system.client.ai.impl;

import com.xy.common.core.domain.R;
import com.xy.system.client.ai.AiServiceClient;
import com.xy.system.config.ai.AiServiceClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * xy-ai 服务内部调用客户端实现
 */
@Slf4j
@Component
public class AiServiceClientImpl implements AiServiceClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestTemplate restTemplate;
    private final AiServiceClientConfig config;

    public AiServiceClientImpl(AiServiceClientConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean syncDocument(Long kbId, String title, String sourceType, String sourceUrl, String content) {
        String url = config.getUrl() + "/ai/knowledge/document";
        Map<String, Object> body = new HashMap<>();
        body.put("kbId", kbId);
        body.put("title", title);
        body.put("sourceType", sourceType);
        body.put("sourceUrl", sourceUrl);
        body.put("content", content);

        HttpHeaders headers = new HttpHeaders();
        headers.set(INTERNAL_API_KEY_HEADER, config.getInternalApiKey());
        log.info("调用 xy-ai 同步文档: url={}, internalApiKeyEmpty={}", url, !StringUtils.hasText(config.getInternalApiKey()));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<R<Boolean>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });
            R<Boolean> result = response.getBody();
            return result != null && result.isOk() && Boolean.TRUE.equals(result.getData());
        } catch (Exception e) {
            log.warn("调用 xy-ai 同步文档失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removeDocument(Long docId) {
        String url = config.getUrl() + "/ai/knowledge/document/" + docId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(INTERNAL_API_KEY_HEADER, config.getInternalApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<R<Boolean>> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });
            R<Boolean> result = response.getBody();
            return result != null && result.isOk() && Boolean.TRUE.equals(result.getData());
        } catch (Exception e) {
            log.warn("调用 xy-ai 删除文档失败: {}", e.getMessage());
            return false;
        }
    }
}
