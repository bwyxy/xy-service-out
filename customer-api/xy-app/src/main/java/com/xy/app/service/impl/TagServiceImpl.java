package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.app.mapper.TagMapper;
import com.xy.app.mapper.TaskMapper;
import com.xy.app.pojo.dto.TodoTagSaveDTO;
import com.xy.app.pojo.vo.TodoTagVO;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.ITagService;
import com.xy.common.entity.todo.TodoTag;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.ContentCheckUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端清单服务实现
 * */
@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, TodoTag> implements ITagService {

    /** 清单名称长度上限（对齐 t_todo_tag.tag_name varchar(24)） */
    private static final int TAG_NAME_MAX_LENGTH = 24;

    /** 首次登录初始化的默认清单：名称 + 图标 */
    private static final String[][] DEFAULT_TAGS = {
            {"工作", "briefcase"},
            {"生活", "coffee"},
            {"阅读", "book"},
            {"个人成长", "sprout"}
    };

    private final TaskMapper taskMapper;

    @Override
    public List<TodoTagVO> listTags() {
        Long uId = TodoUserDetails.getCurrentUId();

        List<TodoTag> tags = list(new LambdaQueryWrapper<TodoTag>()
                .eq(TodoTag::getUserId, uId)
                .eq(TodoTag::getArchived, 0)
                .orderByAsc(TodoTag::getSortNo));

        Map<Long, Long> pendingCounts = new HashMap<>();
        for (TodoTagVO row : taskMapper.countPendingGroupByTag(uId)) {
            if (row.getTagId() != null) {
                pendingCounts.put(row.getTagId(), row.getPendingCount() == null ? 0L : row.getPendingCount());
            }
        }

        return tags.stream()
                .map(tag -> toVO(tag, pendingCounts.getOrDefault(tag.getTagId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoTagVO createTag(TodoTagSaveDTO dto) {
        Long uId = TodoUserDetails.getCurrentUId();

        LocalDateTime now = LocalDateTime.now();

        TodoTag tag = new TodoTag();
        tag.setUserId(uId);
        tag.setTagName(checkTagName(uId, dto.getTagName(), null));
        tag.setIcon(dto.getIcon() == null ? "" : dto.getIcon());
        tag.setColor(dto.getColor() == null ? "" : dto.getColor());
        tag.setSortNo(dto.getSortNo() == null ? nextSortNo(uId) : dto.getSortNo());
        tag.setIsDefault(0);
        tag.setArchived(0);
        tag.setDeleted(0);
        tag.setCreateBy(String.valueOf(uId));
        tag.setUpdateBy(String.valueOf(uId));
        tag.setCreateTime(now);
        tag.setUpdateTime(now);

        save(tag);

        // 新清单下必然没有任务，待完成数固定为 0
        return toVO(tag, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(Long tagId, TodoTagSaveDTO dto) {
        Long uId = TodoUserDetails.getCurrentUId();

        TodoTag exist = getOwnTag(uId, tagId);
        String tagName = checkTagName(uId, dto.getTagName(), tagId);

        TodoTag update = new TodoTag();
        update.setTagName(tagName);
        update.setIcon(dto.getIcon() == null ? exist.getIcon() : dto.getIcon());
        update.setColor(dto.getColor() == null ? exist.getColor() : dto.getColor());
        update.setSortNo(dto.getSortNo() == null ? exist.getSortNo() : dto.getSortNo());
        update.setUpdateBy(String.valueOf(uId));
        update.setUpdateTime(LocalDateTime.now());

        update(update, new LambdaUpdateWrapper<TodoTag>()
                .eq(TodoTag::getTagId, tagId)
                .eq(TodoTag::getUserId, uId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTag(Long tagId) {
        Long uId = TodoUserDetails.getCurrentUId();

        getOwnTag(uId, tagId);

        // 清单下的任务改为未归类，任务本身不删
        taskMapper.detachTag(uId, tagId);

        remove(new LambdaQueryWrapper<TodoTag>()
                .eq(TodoTag::getTagId, tagId)
                .eq(TodoTag::getUserId, uId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultTags(Long uId) {
        Long exists = baseMapper.selectCount(new LambdaQueryWrapper<TodoTag>()
                .eq(TodoTag::getUserId, uId)
                .eq(TodoTag::getIsDefault, 1));

        if (exists != null && exists > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < DEFAULT_TAGS.length; i++) {
            TodoTag tag = new TodoTag();
            tag.setUserId(uId);
            tag.setTagName(DEFAULT_TAGS[i][0]);
            tag.setIcon(DEFAULT_TAGS[i][1]);
            tag.setColor("");
            tag.setSortNo(i + 1);
            tag.setIsDefault(1);
            tag.setArchived(0);
            tag.setDeleted(0);
            tag.setCreateBy(String.valueOf(uId));
            tag.setUpdateBy(String.valueOf(uId));
            tag.setCreateTime(now);
            tag.setUpdateTime(now);
            save(tag);
        }
    }

    /**
     * 取当前用户自己的清单，不存在直接报错（顺带挡住越权改别人的清单）
     * */
    private TodoTag getOwnTag(Long uId, Long tagId) {
        if (tagId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "清单 Id 不能为空");
        }

        TodoTag tag = getOne(new LambdaQueryWrapper<TodoTag>()
                .eq(TodoTag::getTagId, tagId)
                .eq(TodoTag::getUserId, uId), false);

        if (tag == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "清单不存在");
        }

        return tag;
    }

    /**
     * 校验清单名（去空格、长度、敏感词，并挡住同一用户下的同名清单）
     * */
    private String checkTagName(Long uId, String rawTagName, Long excludeTagId) {
        String tagName = rawTagName == null ? null : rawTagName.trim();
        ContentCheckUtils.check(tagName, 1, TAG_NAME_MAX_LENGTH);

        LambdaQueryWrapper<TodoTag> queryWrapper = new LambdaQueryWrapper<TodoTag>()
                .eq(TodoTag::getUserId, uId)
                .eq(TodoTag::getTagName, tagName);

        if (excludeTagId != null) {
            queryWrapper.ne(TodoTag::getTagId, excludeTagId);
        }

        if (getOne(queryWrapper, false) != null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "已存在同名清单");
        }

        return tagName;
    }

    /**
     * 取当前用户最大排序 + 1，新清单排到末尾
     * */
    private int nextSortNo(Long uId) {
        TodoTag last = getOne(new LambdaQueryWrapper<TodoTag>()
                .eq(TodoTag::getUserId, uId)
                .orderByDesc(TodoTag::getSortNo)
                .last("limit 1"), false);

        return last == null || last.getSortNo() == null ? 1 : last.getSortNo() + 1;
    }

    private TodoTagVO toVO(TodoTag tag, Long pendingCount) {
        TodoTagVO vo = new TodoTagVO();
        vo.setTagId(tag.getTagId());
        vo.setTagName(tag.getTagName());
        vo.setIcon(tag.getIcon());
        vo.setColor(tag.getColor());
        vo.setSortNo(tag.getSortNo());
        vo.setIsDefault(tag.getIsDefault());
        vo.setPendingCount(pendingCount);
        vo.setCreateTime(tag.getCreateTime());

        return vo;
    }
}
