package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.ai.ApiKeyGenerateRequest;
import com.xy.common.dto.ai.ApiKeyGenerateResponse;
import com.xy.common.dto.ai.ApiKeyPageQuery;
import com.xy.common.dto.ai.ApiKeyVo;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.ISysAiApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI API 密钥管理控制层
 */
@RestController
@RequestMapping("/ai/api-key")
@RequiredArgsConstructor
public class AiApiKeyController {

    private final ISysAiApiKeyService apiKeyService;

    /**
     * 生成 AI API 密钥对
     */
    @PreAuthorize("@auth.hasPermi('ai:apiKey:add')")
    @Log(title = "AI API 密钥管理", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public R<ApiKeyGenerateResponse> generate(@RequestBody ApiKeyGenerateRequest request) {
        return R.ok(apiKeyService.generate(request.getAppName(), request.getTokenExpireSeconds()));
    }

    /**
     * 分页查询 AI API 密钥列表
     */
    @PreAuthorize("@auth.hasPermi('ai:apiKey:list')")
    @PostMapping("/page")
    public R<PageResult<ApiKeyVo>> page(@RequestBody ApiKeyPageQuery query) {
        return R.ok(apiKeyService.pageKeys(query));
    }

    /**
     * 启用 AI API 密钥
     */
    @PreAuthorize("@auth.hasPermi('ai:apiKey:edit')")
    @Log(title = "AI API 密钥管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/enable")
    public R<Void> enable(@PathVariable Long id) {
        apiKeyService.enable(id);
        return R.ok();
    }

    /**
     * 禁用 AI API 密钥
     */
    @PreAuthorize("@auth.hasPermi('ai:apiKey:edit')")
    @Log(title = "AI API 密钥管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/disable")
    public R<Void> disable(@PathVariable Long id) {
        apiKeyService.disable(id);
        return R.ok();
    }

    /**
     * 删除 AI API 密钥
     */
    @PreAuthorize("@auth.hasPermi('ai:apiKey:remove')")
    @Log(title = "AI API 密钥管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        apiKeyService.delete(id);
        return R.ok();
    }
}
