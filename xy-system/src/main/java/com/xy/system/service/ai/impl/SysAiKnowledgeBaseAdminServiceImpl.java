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
import com.xy.system.service.ai.ISysAiKnowledgeBaseAdminService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 知识库后台管理服务实现
 */
@Service
public class SysAiKnowledgeBaseAdminServiceImpl implements ISysAiKnowledgeBaseAdminService {

    private final AiKnowledgeBaseMapper knowledgeBaseMapper;
    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeBaseVersionMapper versionMapper;
    private final AiKnowledgeBasePermissionMapper permissionMapper;

    public SysAiKnowledgeBaseAdminServiceImpl(AiKnowledgeBaseMapper knowledgeBaseMapper,
                                              AiKnowledgeDocumentMapper documentMapper,
                                              AiKnowledgeBaseVersionMapper versionMapper,
                                              AiKnowledgeBasePermissionMapper permissionMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.documentMapper = documentMapper;
        this.versionMapper = versionMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public PageResult<KnowledgeBaseVo> pageKbs(KnowledgeBasePageQuery query) {
        LambdaQueryWrapper<AiKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getKeyword()), AiKnowledgeBase::getName, query.getKeyword())
                .eq(query.getStatus() != null, AiKnowledgeBase::getStatus, query.getStatus())
                .orderByDesc(AiKnowledgeBase::getCreateTime);

        Page<AiKnowledgeBase> page = knowledgeBaseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<KnowledgeBaseVo> records = page.getRecords().stream()
                .map(this::convertKbToVo)
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public KnowledgeBaseVo getKbById(Long id) {
        AiKnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new ServiceException("知识库不存在");
        }
        return convertKbToVo(kb);
    }

    @Override
    public R<Void> saveOrUpdateKb(KnowledgeBaseRequest request) {
        AiKnowledgeBase entity = new AiKnowledgeBase();
        BeanUtils.copyProperties(request, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(KnowledgeBaseStatus.ENABLED.getCode());
        }
        entity.setUpdateTime(LocalDateTime.now());

        if (entity.getId() == null) {
            LambdaQueryWrapper<AiKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiKnowledgeBase::getName, entity.getName());
            if (knowledgeBaseMapper.selectCount(wrapper) > 0) {
                throw new ServiceException("知识库名称已存在");
            }
            entity.setCreateTime(LocalDateTime.now());
            knowledgeBaseMapper.insert(entity);
        } else {
            knowledgeBaseMapper.updateById(entity);
        }
        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deleteKb(Long id) {
        LambdaQueryWrapper<AiKnowledgeDocument> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(AiKnowledgeDocument::getKbId, id);
        documentMapper.delete(docWrapper);

        LambdaQueryWrapper<AiKnowledgeBaseVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(AiKnowledgeBaseVersion::getKbId, id);
        versionMapper.delete(versionWrapper);

        LambdaQueryWrapper<AiKnowledgeBasePermission> permWrapper = new LambdaQueryWrapper<>();
        permWrapper.eq(AiKnowledgeBasePermission::getKbId, id);
        permissionMapper.delete(permWrapper);

        if (knowledgeBaseMapper.deleteById(id) <= 0) {
            throw new ServiceException("知识库不存在或删除失败");
        }
        return R.ok();
    }

    @Override
    public R<Void> toggleKb(Long id) {
        AiKnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new ServiceException("知识库不存在");
        }
        kb.setStatus(kb.getStatus() == null || kb.getStatus() == KnowledgeBaseStatus.ENABLED.getCode()
                ? KnowledgeBaseStatus.DISABLED.getCode()
                : KnowledgeBaseStatus.ENABLED.getCode());
        kb.setUpdateTime(LocalDateTime.now());
        knowledgeBaseMapper.updateById(kb);
        return R.ok();
    }

    @Override
    public PageResult<KnowledgeDocumentVo> pageDocuments(Long kbId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeDocument::getKbId, kbId)
                .orderByDesc(AiKnowledgeDocument::getCreateTime);

        Page<AiKnowledgeDocument> page = documentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<KnowledgeDocumentVo> records = page.getRecords().stream()
                .map(this::convertDocToVo)
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), (long) pageNum, (long) pageSize);
    }

    @Override
    public KnowledgeDocumentVo getDocumentById(Long id) {
        AiKnowledgeDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        return convertDocToVo(doc);
    }

    @Override
    public KnowledgeDocumentVo saveOrUpdateDocument(KnowledgeDocumentRequest request) {
        AiKnowledgeBase kb = knowledgeBaseMapper.selectById(request.getKbId());
        if (kb == null) {
            throw new ServiceException("知识库不存在");
        }

        AiKnowledgeDocument document;
        if (request.getId() == null) {
            document = new AiKnowledgeDocument();
            BeanUtils.copyProperties(request, document);
            document.setStatus(DocumentStatus.PROCESSING.getCode());
            document.setCreateTime(LocalDateTime.now());
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.insert(document);
        } else {
            document = documentMapper.selectById(request.getId());
            if (document == null) {
                throw new ServiceException("文档不存在");
            }
            BeanUtils.copyProperties(request, document);
            document.setStatus(DocumentStatus.PROCESSING.getCode());
            document.setErrorMsg("");
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
        }
        return convertDocToVo(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deleteDocument(Long id) {
        if (documentMapper.deleteById(id) <= 0) {
            throw new ServiceException("文档不存在或删除失败");
        }
        return R.ok();
    }

    @Override
    public KnowledgeDocumentVo resetDocumentProcessing(Long id) {
        AiKnowledgeDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new ServiceException("文档不存在");
        }
        document.setStatus(DocumentStatus.PROCESSING.getCode());
        document.setErrorMsg("");
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
        return convertDocToVo(document);
    }

    @Override
    public List<KnowledgeBaseVersionVo> listVersions(Long kbId) {
        LambdaQueryWrapper<AiKnowledgeBaseVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeBaseVersion::getKbId, kbId)
                .orderByDesc(AiKnowledgeBaseVersion::getCreateTime);
        return versionMapper.selectList(wrapper).stream()
                .map(this::convertVersionToVo)
                .collect(Collectors.toList());
    }

    @Override
    public R<Void> saveOrUpdateVersion(KnowledgeBaseVersionRequest request) {
        AiKnowledgeBase kb = knowledgeBaseMapper.selectById(request.getKbId());
        if (kb == null) {
            throw new ServiceException("知识库不存在");
        }

        AiKnowledgeBaseVersion entity = new AiKnowledgeBaseVersion();
        BeanUtils.copyProperties(request, entity);
        entity.setStatus(0);
        entity.setUpdateTime(LocalDateTime.now());

        if (entity.getId() == null) {
            LambdaQueryWrapper<AiKnowledgeBaseVersion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiKnowledgeBaseVersion::getKbId, entity.getKbId())
                    .eq(AiKnowledgeBaseVersion::getVersion, entity.getVersion());
            if (versionMapper.selectCount(wrapper) > 0) {
                throw new ServiceException("该版本号已存在");
            }
            entity.setCreateTime(LocalDateTime.now());
            versionMapper.insert(entity);
        } else {
            versionMapper.updateById(entity);
        }
        return R.ok();
    }

    @Override
    public R<Void> publishVersion(Long id) {
        AiKnowledgeBaseVersion version = versionMapper.selectById(id);
        if (version == null) {
            throw new ServiceException("版本不存在");
        }
        version.setStatus(1);
        version.setPublishedTime(LocalDateTime.now());
        version.setUpdateTime(LocalDateTime.now());
        versionMapper.updateById(version);
        return R.ok();
    }

    @Override
    public R<Void> archiveVersion(Long id) {
        AiKnowledgeBaseVersion version = versionMapper.selectById(id);
        if (version == null) {
            throw new ServiceException("版本不存在");
        }
        version.setStatus(2);
        version.setUpdateTime(LocalDateTime.now());
        versionMapper.updateById(version);
        return R.ok();
    }

    @Override
    public R<Void> deleteVersion(Long id) {
        if (versionMapper.deleteById(id) <= 0) {
            throw new ServiceException("版本不存在或删除失败");
        }
        return R.ok();
    }

    @Override
    public List<KnowledgeBasePermissionVo> listPermissions(Long kbId) {
        LambdaQueryWrapper<AiKnowledgeBasePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeBasePermission::getKbId, kbId)
                .orderByDesc(AiKnowledgeBasePermission::getCreateTime);
        return permissionMapper.selectList(wrapper).stream()
                .map(this::convertPermissionToVo)
                .collect(Collectors.toList());
    }

    @Override
    public R<Void> grantPermission(KnowledgeBasePermissionRequest request) {
        AiKnowledgeBase kb = knowledgeBaseMapper.selectById(request.getKbId());
        if (kb == null) {
            throw new ServiceException("知识库不存在");
        }

        LambdaQueryWrapper<AiKnowledgeBasePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeBasePermission::getKbId, request.getKbId())
                .eq(AiKnowledgeBasePermission::getTargetType, request.getTargetType())
                .eq(AiKnowledgeBasePermission::getTargetId, request.getTargetId());
        if (permissionMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该授权对象已存在");
        }

        AiKnowledgeBasePermission entity = new AiKnowledgeBasePermission();
        BeanUtils.copyProperties(request, entity);
        entity.setCreateTime(LocalDateTime.now());
        permissionMapper.insert(entity);
        return R.ok();
    }

    @Override
    public R<Void> revokePermission(Long id) {
        if (permissionMapper.deleteById(id) <= 0) {
            throw new ServiceException("权限不存在或删除失败");
        }
        return R.ok();
    }

    private KnowledgeBaseVo convertKbToVo(AiKnowledgeBase kb) {
        KnowledgeBaseVo vo = new KnowledgeBaseVo();
        BeanUtils.copyProperties(kb, vo);
        return vo;
    }

    private KnowledgeDocumentVo convertDocToVo(AiKnowledgeDocument doc) {
        KnowledgeDocumentVo vo = new KnowledgeDocumentVo();
        BeanUtils.copyProperties(doc, vo);
        return vo;
    }

    private KnowledgeBaseVersionVo convertVersionToVo(AiKnowledgeBaseVersion version) {
        KnowledgeBaseVersionVo vo = new KnowledgeBaseVersionVo();
        BeanUtils.copyProperties(version, vo);
        return vo;
    }

    private KnowledgeBasePermissionVo convertPermissionToVo(AiKnowledgeBasePermission permission) {
        KnowledgeBasePermissionVo vo = new KnowledgeBasePermissionVo();
        BeanUtils.copyProperties(permission, vo);
        return vo;
    }
}
