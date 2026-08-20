package com.chat.ai.controller;

import com.chat.ai.dto.ModelProviderRequest;
import com.chat.ai.service.AiProviderService;
import com.chat.ai.vo.ModelProviderVo;
import com.xy.common.core.domain.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 模型提供者管理接口
 */
@RestController
@RequestMapping("/ai/provider")
public class AiProviderController {

    private final AiProviderService providerService;

    public AiProviderController(AiProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping("/list")
    public R<List<ModelProviderVo>> list() {
        return R.ok(providerService.listProviders());
    }

    @PostMapping
    public R<Boolean> saveOrUpdate(@Valid @RequestBody ModelProviderRequest request) {
        return R.ok(providerService.saveOrUpdateProvider(request));
    }

    @PutMapping("/{id}/toggle")
    public R<Boolean> toggle(@PathVariable Long id) {
        return R.ok(providerService.toggleProvider(id));
    }
}
