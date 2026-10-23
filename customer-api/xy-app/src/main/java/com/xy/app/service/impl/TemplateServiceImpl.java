package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.app.mapper.TagMapper;
import com.xy.app.mapper.TaskMapper;
import com.xy.app.mapper.TemplateMapper;
import com.xy.app.pojo.dto.TodoTagSaveDTO;
import com.xy.app.pojo.dto.TodoTaskCreateDTO;
import com.xy.app.pojo.dto.TodoTemplateApplyDTO;
import com.xy.app.pojo.vo.TodoTagVO;
import com.xy.app.pojo.vo.TodoTemplateApplyVO;
import com.xy.app.pojo.vo.TodoTemplateVO;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.ITagService;
import com.xy.app.service.ITaskService;
import com.xy.app.service.ITemplateService;
import com.xy.common.entity.todo.TodoTag;
import com.xy.common.entity.todo.TodoTask;
import com.xy.common.entity.todo.TodoTemplate;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端清单模板服务实现
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, TodoTemplate> implements ITemplateService {

    /** 模板状态：上架 */
    private static final int STATUS_ENABLED = 1;

    /** 套用模板生成任务的幂等 Id 前缀，同一批次内按序号拼成 clientId */
    private static final String CLIENT_ID_PREFIX = "tpl-";

    /** 清单名称长度上限（对齐 t_todo_tag.tag_name varchar(24)） */
    private static final int TAG_NAME_MAX_LENGTH = 24;

    /** 清单重名时最多尝试到几号后缀 */
    private static final int MAX_NAME_SUFFIX = 9999;

    /** 清单重名时补序号用的兜底名称 */
    private static final String FALLBACK_TAG_NAME = "新清单";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ITagService tagService;

    private final ITaskService taskService;

    private final TagMapper tagMapper;

    private final TaskMapper taskMapper;

    @Override
    public List<TodoTemplateVO> listTemplates() {
        List<TodoTemplate> templates = list(new LambdaQueryWrapper<TodoTemplate>()
                .eq(TodoTemplate::getStatus, STATUS_ENABLED)
                .orderByAsc(TodoTemplate::getSortNo)
                .orderByAsc(TodoTemplate::getTemplateId));

        return templates.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoTemplateApplyVO applyTemplate(String code, TodoTemplateApplyDTO dto) {
        Long uId = TodoUserDetails.getCurrentUId();

        TodoTemplate template = getEnabledTemplate(code);
        List<TemplateItem> items = parseItems(template.getItems());
        if (items.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "模板内容为空，暂时无法套用");
        }

        String batchId = resolveBatchId(dto);

        // 幂等：同一批次已经建过任务，直接把上次的结果回给前端，避免连点建出第二个清单
        TodoTemplateApplyVO applied = findApplied(uId, batchId);
        if (applied != null) {
            return applied;
        }

        TodoTagSaveDTO tagDTO = new TodoTagSaveDTO();
        tagDTO.setTagName(resolveTagName(uId, template.getName()));
        tagDTO.setIcon(template.getIcon() == null ? "" : template.getIcon());

        TodoTagVO tag = tagService.createTag(tagDTO);

        for (int i = 0; i < items.size(); i++) {
            TodoTaskCreateDTO createDTO = new TodoTaskCreateDTO();
            createDTO.setClientId(CLIENT_ID_PREFIX + batchId + "-" + i);
            createDTO.setTitle(items.get(i).getTitle());
            createDTO.setPriority(items.get(i).getPriority());
            createDTO.setTagId(tag.getTagId());

            taskService.createTask(createDTO);
        }

        TodoTemplateApplyVO vo = new TodoTemplateApplyVO();
        vo.setTagId(tag.getTagId());
        vo.setTagName(tag.getTagName());
        vo.setTaskCount(items.size());

        return vo;
    }

    /**
     * 取上架中的模板，编码对不上直接报错
     * */
    private TodoTemplate getEnabledTemplate(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "模板编码不能为空");
        }

        TodoTemplate template = getOne(new LambdaQueryWrapper<TodoTemplate>()
                .eq(TodoTemplate::getCode, code.trim())
                .eq(TodoTemplate::getStatus, STATUS_ENABLED), false);

        if (template == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "模板不存在或已下架");
        }

        return template;
    }

    /**
     * 取批次号：前端没传时兜底生成一个，保证本次调用内部仍按 clientId 幂等
     * */
    private String resolveBatchId(TodoTemplateApplyDTO dto) {
        String batchId = dto == null ? null : dto.getBatchId();

        if (!StringUtils.hasText(batchId)) {
            return UUID.randomUUID().toString().replace("-", "");
        }

        return batchId.trim();
    }

    /**
     * 按批次号回查是否已经套用过（有该批次的任务即视为已套用）
     * */
    private TodoTemplateApplyVO findApplied(Long uId, String batchId) {
        List<TodoTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<TodoTask>()
                .select(TodoTask::getTaskId, TodoTask::getTagId)
                .eq(TodoTask::getUserId, uId)
                .eq(TodoTask::getDeleted, 0)
                .likeRight(TodoTask::getClientId, CLIENT_ID_PREFIX + batchId + "-"));

        if (tasks.isEmpty()) {
            return null;
        }

        Long tagId = tasks.get(0).getTagId();
        TodoTag tag = tagId == null ? null : tagMapper.selectById(tagId);

        TodoTemplateApplyVO vo = new TodoTemplateApplyVO();
        vo.setTagId(tagId);
        vo.setTagName(tag == null ? "" : tag.getTagName());
        vo.setTaskCount(tasks.size());

        return vo;
    }

    /**
     * 定清单名：同一用户下清单名唯一，模板名被占用时补序号（如「晨间计划2」）
     * */
    private String resolveTagName(Long uId, String templateName) {
        String base = StringUtils.hasText(templateName) ? templateName.trim() : FALLBACK_TAG_NAME;

        Set<String> exists = new HashSet<>();
        List<TodoTag> tags = tagMapper.selectList(new LambdaQueryWrapper<TodoTag>()
                .select(TodoTag::getTagName)
                .eq(TodoTag::getUserId, uId));

        for (TodoTag tag : tags) {
            if (tag.getTagName() != null) {
                exists.add(tag.getTagName());
            }
        }

        if (!exists.contains(base)) {
            return base;
        }

        for (int i = 2; i <= MAX_NAME_SUFFIX; i++) {
            String suffix = String.valueOf(i);
            String candidate = trimToLimit(base, TAG_NAME_MAX_LENGTH - suffix.length()) + suffix;
            if (!exists.contains(candidate)) {
                return candidate;
            }
        }

        throw new BusinessException(ResultCode.OPERATION_FAILED, "同名清单太多了，请先整理一下已有清单");
    }

    /**
     * 套用模板本质是新建清单，清单名超长时只截断模板名部分，保证拼上序号后不超上限
     * */
    private String trimToLimit(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    /**
     * 解析模板预置任务 JSON，格式异常时按空模板处理（宁可提示「模板内容为空」也不要 500）
     * */
    private List<TemplateItem> parseItems(String items) {
        if (!StringUtils.hasText(items)) {
            return List.of();
        }

        try {
            List<TemplateItem> parsed = OBJECT_MAPPER.readValue(items, new TypeReference<>() {
            });

            if (parsed == null) {
                return List.of();
            }

            return parsed.stream()
                    .filter(item -> item != null && StringUtils.hasText(item.getTitle()))
                    .toList();
        } catch (JsonProcessingException e) {
            log.warn("模板预置任务 JSON 解析失败，按空模板处理 items={}", items, e);

            return List.of();
        }
    }

    private TodoTemplateVO toVO(TodoTemplate template) {
        TodoTemplateVO vo = new TodoTemplateVO();
        vo.setTemplateId(template.getTemplateId());
        vo.setCode(template.getCode());
        vo.setName(template.getName());
        vo.setDescription(template.getDescription());
        vo.setCategory(template.getCategory());
        vo.setIcon(template.getIcon());
        vo.setItemCount(parseItems(template.getItems()).size());

        return vo;
    }

    /**
     * 模板预置任务（items JSON 数组里的一个元素）
     * */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TemplateItem {

        /**
         * 任务标题
         * */
        private String title;

        /**
         * 优先级 0-无 1-普通 2-重要
         * */
        private Integer priority;
    }
}
