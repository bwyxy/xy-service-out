package com.xy.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.dto.ai.ApiKeyGenerateResponse;
import com.xy.common.dto.ai.ApiKeyPageQuery;
import com.xy.common.dto.ai.ApiKeyVo;
import com.xy.common.dto.ai.TokenResponse;
import com.xy.common.entity.AiApiKey;
import com.xy.common.core.domain.PageResult;

/**
 * AI API 密钥服务接口
 */
public interface ISysAiApiKeyService extends IService<AiApiKey> {

    /**
     * 通过 API Key 和 API Secret 换取访问令牌
     *
     * @param apiKey    访问密钥 Key
     * @param apiSecret 访问密钥 Secret
     * @return Token 响应
     */
    TokenResponse exchangeToken(String apiKey, String apiSecret);

    /**
     * 根据 API Key 查询密钥信息
     *
     * @param apiKey 访问密钥 Key
     * @return 密钥实体
     */
    AiApiKey getByApiKey(String apiKey);

    /**
     * 生成新的 API 密钥对
     *
     * @param appName            应用名称
     * @param tokenExpireSeconds token 有效期（秒）
     * @return 生成的密钥对响应
     */
    ApiKeyGenerateResponse generate(String appName, Integer tokenExpireSeconds);

    /**
     * 分页查询 API 密钥列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ApiKeyVo> pageKeys(ApiKeyPageQuery query);

    /**
     * 启用 API 密钥
     *
     * @param id 密钥 ID
     */
    void enable(Long id);

    /**
     * 禁用 API 密钥
     *
     * @param id 密钥 ID
     */
    void disable(Long id);

    /**
     * 删除 API 密钥
     *
     * @param id 密钥 ID
     */
    void delete(Long id);
}
