package com.xy.system.service.ai;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.ai.*;
import com.xy.common.vo.ai.KnowledgeBasePermissionVo;
import com.xy.common.vo.ai.KnowledgeBaseVersionVo;
import com.xy.common.vo.ai.KnowledgeDocumentVo;
import com.xy.common.vo.ai.KnowledgeBaseVo;

import java.util.List;

/**
 * AI 知识库后台管理服务
 */
public interface ISysAiKnowledgeBaseAdminService {

    PageResult<KnowledgeBaseVo> pageKbs(KnowledgeBasePageQuery query);

    KnowledgeBaseVo getKbById(Long id);

    R<Void> saveOrUpdateKb(KnowledgeBaseRequest request);

    R<Void> deleteKb(Long id);

    R<Void> toggleKb(Long id);

    PageResult<KnowledgeDocumentVo> pageDocuments(Long kbId, Integer pageNum, Integer pageSize);

    KnowledgeDocumentVo getDocumentById(Long id);

    /**
     * 保存或更新文档元数据，并返回文档视图对象
     */
    KnowledgeDocumentVo saveOrUpdateDocument(KnowledgeDocumentRequest request);

    R<Void> deleteDocument(Long id);

    /**
     * 将文档状态重置为处理中并返回文档视图对象，方便外部触发重新向量化
     */
    KnowledgeDocumentVo resetDocumentProcessing(Long id);

    List<KnowledgeBaseVersionVo> listVersions(Long kbId);

    R<Void> saveOrUpdateVersion(KnowledgeBaseVersionRequest request);

    R<Void> publishVersion(Long id);

    R<Void> archiveVersion(Long id);

    R<Void> deleteVersion(Long id);

    List<KnowledgeBasePermissionVo> listPermissions(Long kbId);

    R<Void> grantPermission(KnowledgeBasePermissionRequest request);

    R<Void> revokePermission(Long id);
}
