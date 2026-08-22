package com.xy.system.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.ai.*;
import com.xy.common.entity.ai.*;
import com.xy.common.enums.ai.DocumentStatus;
import com.xy.common.enums.ai.KnowledgeBaseStatus;
import com.xy.common.exception.ServiceException;
import com.xy.common.vo.ai.KnowledgeBasePermissionVo;
import com.xy.common.vo.ai.KnowledgeBaseVersionVo;
import com.xy.common.vo.ai.KnowledgeDocumentVo;
import com.xy.common.vo.ai.KnowledgeBaseVo;
import com.xy.system.mapper.ai.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AI 知识库后台管理服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class SysAiKnowledgeBaseAdminServiceImplTest {

    @Mock
    private AiKnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private AiKnowledgeDocumentMapper documentMapper;

    @Mock
    private AiKnowledgeBaseVersionMapper versionMapper;

    @Mock
    private AiKnowledgeBasePermissionMapper permissionMapper;

    @InjectMocks
    private SysAiKnowledgeBaseAdminServiceImpl knowledgeBaseAdminService;

    @Test
    void pageKbs_shouldReturnPagedResult() {
        KnowledgeBasePageQuery query = new KnowledgeBasePageQuery();
        query.setKeyword("产品");
        query.setPageNum(1);
        query.setPageSize(10);

        AiKnowledgeBase kb = createKb(1L, "产品手册", KnowledgeBaseStatus.ENABLED.getCode());
        Page<AiKnowledgeBase> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(kb));

        when(knowledgeBaseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<KnowledgeBaseVo> result = knowledgeBaseAdminService.pageKbs(query);

        assertEquals(1L, result.getTotal());
        assertEquals("产品手册", result.getList().get(0).getName());
    }

    @Test
    void getKbById_shouldReturnKbWhenExists() {
        AiKnowledgeBase kb = createKb(1L, "产品手册", KnowledgeBaseStatus.ENABLED.getCode());
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(kb);

        KnowledgeBaseVo result = knowledgeBaseAdminService.getKbById(1L);

        assertEquals("产品手册", result.getName());
    }

    @Test
    void saveOrUpdateKb_shouldInsertWhenIdIsNull() {
        KnowledgeBaseRequest request = new KnowledgeBaseRequest();
        request.setName("新产品库");
        request.setDescription("描述");

        when(knowledgeBaseMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(knowledgeBaseMapper.insert(any(AiKnowledgeBase.class))).thenReturn(1);

        R<Void> result = knowledgeBaseAdminService.saveOrUpdateKb(request);

        assertTrue(result.isOk());
        verify(knowledgeBaseMapper, times(1)).insert(any(AiKnowledgeBase.class));
    }

    @Test
    void toggleKb_shouldSwitchStatus() {
        AiKnowledgeBase kb = createKb(1L, "产品手册", KnowledgeBaseStatus.ENABLED.getCode());
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(kb);
        when(knowledgeBaseMapper.updateById(any(AiKnowledgeBase.class))).thenReturn(1);

        R<Void> result = knowledgeBaseAdminService.toggleKb(1L);

        assertTrue(result.isOk());
        assertEquals(KnowledgeBaseStatus.DISABLED.getCode(), kb.getStatus());
    }

    @Test
    void pageDocuments_shouldReturnPagedResult() {
        AiKnowledgeDocument doc = createDocument(1L, 1L, "产品介绍", DocumentStatus.COMPLETED.getCode());
        Page<AiKnowledgeDocument> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(doc));

        when(documentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<KnowledgeDocumentVo> result = knowledgeBaseAdminService.pageDocuments(1L, 1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals("产品介绍", result.getList().get(0).getTitle());
    }

    @Test
    void saveOrUpdateDocument_shouldInsertAndReturnVo() {
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest();
        request.setKbId(1L);
        request.setTitle("产品介绍");
        request.setContent("我们的产品是 AI 助手");
        request.setSourceType("TEXT");

        AiKnowledgeBase kb = createKb(1L, "产品手册", KnowledgeBaseStatus.ENABLED.getCode());
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(kb);
        when(documentMapper.insert(any(AiKnowledgeDocument.class))).thenAnswer(invocation -> {
            AiKnowledgeDocument arg = invocation.getArgument(0);
            arg.setId(1L);
            return 1;
        });

        KnowledgeDocumentVo result = knowledgeBaseAdminService.saveOrUpdateDocument(request);

        assertNotNull(result);
        assertEquals("产品介绍", result.getTitle());
        assertEquals(DocumentStatus.PROCESSING.getCode(), result.getStatus());
        verify(documentMapper, times(1)).insert(any(AiKnowledgeDocument.class));
    }

    @Test
    void deleteDocument_shouldRemoveDocument() {
        when(documentMapper.deleteById(1L)).thenReturn(1);

        R<Void> result = knowledgeBaseAdminService.deleteDocument(1L);

        assertTrue(result.isOk());
        verify(documentMapper, times(1)).deleteById(1L);
    }

    @Test
    void listVersions_shouldReturnVersions() {
        AiKnowledgeBaseVersion version = createVersion(1L, 1L, "v1.0.0", 0);
        when(versionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(version));

        List<KnowledgeBaseVersionVo> result = knowledgeBaseAdminService.listVersions(1L);

        assertEquals(1, result.size());
        assertEquals("v1.0.0", result.get(0).getVersion());
    }

    @Test
    void saveOrUpdateVersion_shouldInsertWhenIdIsNull() {
        KnowledgeBaseVersionRequest request = new KnowledgeBaseVersionRequest();
        request.setKbId(1L);
        request.setVersion("v1.0.0");
        request.setDescription("初始版本");

        AiKnowledgeBase kb = createKb(1L, "产品手册", KnowledgeBaseStatus.ENABLED.getCode());
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(kb);
        when(versionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(versionMapper.insert(any(AiKnowledgeBaseVersion.class))).thenReturn(1);

        R<Void> result = knowledgeBaseAdminService.saveOrUpdateVersion(request);

        assertTrue(result.isOk());
        verify(versionMapper, times(1)).insert(any(AiKnowledgeBaseVersion.class));
    }

    @Test
    void publishVersion_shouldSetStatusToPublished() {
        AiKnowledgeBaseVersion version = createVersion(1L, 1L, "v1.0.0", 0);
        when(versionMapper.selectById(1L)).thenReturn(version);
        when(versionMapper.updateById(any(AiKnowledgeBaseVersion.class))).thenReturn(1);

        R<Void> result = knowledgeBaseAdminService.publishVersion(1L);

        assertTrue(result.isOk());
        assertEquals(1, version.getStatus());
        assertNotNull(version.getPublishedTime());
    }

    @Test
    void listPermissions_shouldReturnPermissions() {
        AiKnowledgeBasePermission permission = createPermission(1L, 1L, "ROLE", 1L, "READ");
        when(permissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(permission));

        List<KnowledgeBasePermissionVo> result = knowledgeBaseAdminService.listPermissions(1L);

        assertEquals(1, result.size());
        assertEquals("READ", result.get(0).getPermission());
    }

    @Test
    void grantPermission_shouldInsertPermission() {
        KnowledgeBasePermissionRequest request = new KnowledgeBasePermissionRequest();
        request.setKbId(1L);
        request.setTargetType("ROLE");
        request.setTargetId(1L);
        request.setPermission("READ");

        AiKnowledgeBase kb = createKb(1L, "产品手册", KnowledgeBaseStatus.ENABLED.getCode());
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(kb);
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.insert(any(AiKnowledgeBasePermission.class))).thenReturn(1);

        R<Void> result = knowledgeBaseAdminService.grantPermission(request);

        assertTrue(result.isOk());
        verify(permissionMapper, times(1)).insert(any(AiKnowledgeBasePermission.class));
    }

    @Test
    void grantPermission_shouldThrowWhenDuplicate() {
        KnowledgeBasePermissionRequest request = new KnowledgeBasePermissionRequest();
        request.setKbId(1L);
        request.setTargetType("ROLE");
        request.setTargetId(1L);
        request.setPermission("READ");

        AiKnowledgeBase kb = createKb(1L, "产品手册", KnowledgeBaseStatus.ENABLED.getCode());
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(kb);
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> knowledgeBaseAdminService.grantPermission(request));
        assertTrue(exception.getMessage().contains("已存在"));
    }

    private AiKnowledgeBase createKb(Long id, String name, Integer status) {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(id);
        kb.setName(name);
        kb.setStatus(status);
        return kb;
    }

    private AiKnowledgeDocument createDocument(Long id, Long kbId, String title, Integer status) {
        AiKnowledgeDocument doc = new AiKnowledgeDocument();
        doc.setId(id);
        doc.setKbId(kbId);
        doc.setTitle(title);
        doc.setStatus(status);
        return doc;
    }

    private AiKnowledgeBaseVersion createVersion(Long id, Long kbId, String version, Integer status) {
        AiKnowledgeBaseVersion v = new AiKnowledgeBaseVersion();
        v.setId(id);
        v.setKbId(kbId);
        v.setVersion(version);
        v.setStatus(status);
        return v;
    }

    private AiKnowledgeBasePermission createPermission(Long id, Long kbId, String targetType,
                                                       Long targetId, String permission) {
        AiKnowledgeBasePermission p = new AiKnowledgeBasePermission();
        p.setId(id);
        p.setKbId(kbId);
        p.setTargetType(targetType);
        p.setTargetId(targetId);
        p.setPermission(permission);
        return p;
    }
}
