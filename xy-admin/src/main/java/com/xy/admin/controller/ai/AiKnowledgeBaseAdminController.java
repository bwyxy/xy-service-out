package com.xy.admin.controller.ai;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.ai.*;
import com.xy.common.enums.BusinessType;
import com.xy.common.vo.ai.KnowledgeBasePermissionVo;
import com.xy.common.vo.ai.KnowledgeBaseVersionVo;
import com.xy.common.vo.ai.KnowledgeDocumentVo;
import com.xy.common.vo.ai.KnowledgeBaseVo;
import com.xy.system.client.ai.AiServiceClient;
import com.xy.system.service.ai.ISysAiKnowledgeBaseAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 知识库后台管理
 */
@RestController
@RequestMapping("/admin/ai/knowledge")
@RequiredArgsConstructor
public class AiKnowledgeBaseAdminController {

    private final ISysAiKnowledgeBaseAdminService knowledgeBaseAdminService;
    private final AiServiceClient aiServiceClient;

    /**
     * 分页查询知识库
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:list')")
    @GetMapping("/page")
    public R<PageResult<KnowledgeBaseVo>> page(KnowledgeBasePageQuery query) {
        return R.ok(knowledgeBaseAdminService.pageKbs(query));
    }

    /**
     * 查询知识库详情
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:query')")
    @GetMapping("/{id}")
    public R<KnowledgeBaseVo> getById(@PathVariable Long id) {
        return R.ok(knowledgeBaseAdminService.getKbById(id));
    }

    /**
     * 新增知识库
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:add')")
    @Log(title = "AI 知识库管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Valid @RequestBody KnowledgeBaseRequest request) {
        return knowledgeBaseAdminService.saveOrUpdateKb(request);
    }

    /**
     * 编辑知识库
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:edit')")
    @Log(title = "AI 知识库管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Valid @RequestBody KnowledgeBaseRequest request) {
        if (request.getId() == null) {
            return R.error(400, "ID 不能为空");
        }
        return knowledgeBaseAdminService.saveOrUpdateKb(request);
    }

    /**
     * 删除知识库
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:remove')")
    @Log(title = "AI 知识库管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        return knowledgeBaseAdminService.deleteKb(id);
    }

    /**
     * 切换启用状态
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:edit')")
    @Log(title = "AI 知识库管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/toggle")
    public R<Void> toggle(@PathVariable Long id) {
        return knowledgeBaseAdminService.toggleKb(id);
    }

    /**
     * 分页查询文档
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:list')")
    @GetMapping("/{kbId}/document/page")
    public R<PageResult<KnowledgeDocumentVo>> pageDocuments(@PathVariable Long kbId,
                                                            @RequestParam(defaultValue = "1") int pageNum,
                                                            @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(knowledgeBaseAdminService.pageDocuments(kbId, pageNum, pageSize));
    }

    /**
     * 查询文档详情
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:query')")
    @GetMapping("/document/{id}")
    public R<KnowledgeDocumentVo> getDocumentById(@PathVariable Long id) {
        return R.ok(knowledgeBaseAdminService.getDocumentById(id));
    }

    /**
     * 新增文档
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:add')")
    @Log(title = "AI 知识库文档管理", businessType = BusinessType.INSERT)
    @PostMapping("/document")
    public R<Void> addDocument(@Valid @RequestBody KnowledgeDocumentRequest request) {
        KnowledgeDocumentVo document = knowledgeBaseAdminService.saveOrUpdateDocument(request);
        syncDocument(document);
        return R.ok();
    }

    /**
     * 编辑文档
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:edit')")
    @Log(title = "AI 知识库文档管理", businessType = BusinessType.UPDATE)
    @PutMapping("/document")
    public R<Void> editDocument(@Valid @RequestBody KnowledgeDocumentRequest request) {
        if (request.getId() == null) {
            return R.error(400, "ID 不能为空");
        }
        aiServiceClient.removeDocument(request.getId());
        KnowledgeDocumentVo document = knowledgeBaseAdminService.saveOrUpdateDocument(request);
        syncDocument(document);
        return R.ok();
    }

    /**
     * 删除文档
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:remove')")
    @Log(title = "AI 知识库文档管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/document/{id}")
    public R<Void> deleteDocument(@PathVariable Long id) {
        aiServiceClient.removeDocument(id);
        return knowledgeBaseAdminService.deleteDocument(id);
    }

    /**
     * 重新索引文档
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:edit')")
    @Log(title = "AI 知识库文档管理", businessType = BusinessType.UPDATE)
    @PostMapping("/document/{id}/reindex")
    public R<Void> reindexDocument(@PathVariable Long id) {
        KnowledgeDocumentVo document = knowledgeBaseAdminService.resetDocumentProcessing(id);
        aiServiceClient.removeDocument(id);
        syncDocument(document);
        return R.ok();
    }

    /**
     * 查询版本列表
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:query')")
    @GetMapping("/{kbId}/version/list")
    public R<List<KnowledgeBaseVersionVo>> listVersions(@PathVariable Long kbId) {
        return R.ok(knowledgeBaseAdminService.listVersions(kbId));
    }

    /**
     * 新增/编辑版本
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:edit')")
    @Log(title = "AI 知识库版本管理", businessType = BusinessType.UPDATE)
    @PostMapping("/version")
    public R<Void> saveOrUpdateVersion(@Valid @RequestBody KnowledgeBaseVersionRequest request) {
        return knowledgeBaseAdminService.saveOrUpdateVersion(request);
    }

    /**
     * 发布版本
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:edit')")
    @Log(title = "AI 知识库版本管理", businessType = BusinessType.UPDATE)
    @PutMapping("/version/{id}/publish")
    public R<Void> publishVersion(@PathVariable Long id) {
        return knowledgeBaseAdminService.publishVersion(id);
    }

    /**
     * 归档版本
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:edit')")
    @Log(title = "AI 知识库版本管理", businessType = BusinessType.UPDATE)
    @PutMapping("/version/{id}/archive")
    public R<Void> archiveVersion(@PathVariable Long id) {
        return knowledgeBaseAdminService.archiveVersion(id);
    }

    /**
     * 删除版本
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:remove')")
    @Log(title = "AI 知识库版本管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/version/{id}")
    public R<Void> deleteVersion(@PathVariable Long id) {
        return knowledgeBaseAdminService.deleteVersion(id);
    }

    /**
     * 查询权限列表
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:query')")
    @GetMapping("/{kbId}/permission/list")
    public R<List<KnowledgeBasePermissionVo>> listPermissions(@PathVariable Long kbId) {
        return R.ok(knowledgeBaseAdminService.listPermissions(kbId));
    }

    /**
     * 授权
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:edit')")
    @Log(title = "AI 知识库权限管理", businessType = BusinessType.INSERT)
    @PostMapping("/permission")
    public R<Void> grantPermission(@Valid @RequestBody KnowledgeBasePermissionRequest request) {
        return knowledgeBaseAdminService.grantPermission(request);
    }

    /**
     * 撤销权限
     */
    @PreAuthorize("@auth.hasPermi('ai:knowledge:remove')")
    @Log(title = "AI 知识库权限管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/permission/{id}")
    public R<Void> revokePermission(@PathVariable Long id) {
        return knowledgeBaseAdminService.revokePermission(id);
    }

    private void syncDocument(KnowledgeDocumentVo document) {
        boolean success = aiServiceClient.syncDocument(
                document.getKbId(), document.getTitle(),
                document.getSourceType(), document.getSourceUrl(), document.getContent());
        if (!success) {
            throw new com.xy.common.exception.ServiceException("文档元数据已保存，但调用 AI 服务分片失败");
        }
    }
}
