package com.xy.admin.controller.ai;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.ai.AiModelProviderPageQuery;
import com.xy.common.dto.ai.AiModelProviderRequest;
import com.xy.common.enums.BusinessType;
import com.xy.common.vo.ai.AiModelProviderVo;
import com.xy.system.service.ai.ISysAiModelAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI 模型提供者后台管理控制层
 */
@RestController
@RequestMapping("/admin/ai/model")
@RequiredArgsConstructor
public class AiModelAdminController {

    private final ISysAiModelAdminService modelAdminService;

    /**
     * 分页查询模型提供者
     */
    @PreAuthorize("@auth.hasPermi('ai:model:list')")
    @GetMapping("/page")
    public R<PageResult<AiModelProviderVo>> page(AiModelProviderPageQuery query) {
        return R.ok(modelAdminService.pageProviders(query));
    }

    /**
     * 根据 ID 查询模型提供者
     */
    @PreAuthorize("@auth.hasPermi('ai:model:query')")
    @GetMapping("/{id}")
    public R<AiModelProviderVo> getById(@PathVariable Long id) {
        return R.ok(modelAdminService.getProviderById(id));
    }

    /**
     * 新增模型提供者
     */
    @PreAuthorize("@auth.hasPermi('ai:model:add')")
    @Log(title = "AI 模型管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Valid @RequestBody AiModelProviderRequest request) {
        return modelAdminService.saveOrUpdateProvider(request);
    }

    /**
     * 编辑模型提供者
     */
    @PreAuthorize("@auth.hasPermi('ai:model:edit')")
    @Log(title = "AI 模型管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Valid @RequestBody AiModelProviderRequest request) {
        if (request.getId() == null) {
            return R.error(400, "ID 不能为空");
        }
        return modelAdminService.saveOrUpdateProvider(request);
    }

    /**
     * 删除模型提供者
     */
    @PreAuthorize("@auth.hasPermi('ai:model:remove')")
    @Log(title = "AI 模型管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        return modelAdminService.deleteProvider(id);
    }

    /**
     * 切换启用状态
     */
    @PreAuthorize("@auth.hasPermi('ai:model:edit')")
    @Log(title = "AI 模型管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/toggle")
    public R<Void> toggle(@PathVariable Long id) {
        return modelAdminService.toggleProvider(id);
    }
}
