package com.chat.ai.controller;

import com.chat.ai.dto.KnowledgeBaseRequest;
import com.chat.ai.dto.KnowledgeDocumentRequest;
import com.chat.ai.service.AiKnowledgeBaseService;
import com.chat.ai.vo.KnowledgeBaseVo;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 知识库管理接口
 */
@RestController
@RequestMapping("/ai/knowledge")
public class AiKnowledgeBaseController {

    private final AiKnowledgeBaseService knowledgeBaseService;

    public AiKnowledgeBaseController(AiKnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping("/base")
    public R<Boolean> saveOrUpdateBase(@Valid @RequestBody KnowledgeBaseRequest request) {
        return R.ok(knowledgeBaseService.saveOrUpdateKb(request));
    }

    @GetMapping("/base/page")
    public R<PageResult<KnowledgeBaseVo>> pageBase(@RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(knowledgeBaseService.pageKbs(keyword, pageNum, pageSize));
    }

    @PostMapping("/document")
    public R<Boolean> addDocument(@Valid @RequestBody KnowledgeDocumentRequest request) {
        return R.ok(knowledgeBaseService.addDocument(request));
    }

    @DeleteMapping("/document/{id}")
    public R<Boolean> removeDocument(@PathVariable Long id) {
        return R.ok(knowledgeBaseService.removeDocument(id));
    }

    @GetMapping("/retrieve")
    public R<List<String>> retrieve(@RequestParam Long kbId,
                                    @RequestParam String query,
                                    @RequestParam(defaultValue = "5") int topK) {
        return R.ok(knowledgeBaseService.retrieveChunks(kbId, query, topK));
    }
}
