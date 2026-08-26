package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.forum.ForumBoard;
import com.xy.common.entity.forum.ForumModerator;
import com.xy.common.entity.forum.ForumPost;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.forum.ForumBoardMapper;
import com.xy.system.mapper.forum.ForumModeratorMapper;
import com.xy.system.mapper.forum.ForumPostMapper;
import com.xy.system.service.forum.IForumBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 论坛板块 Service 实现
 */
@Service
@RequiredArgsConstructor
public class ForumBoardServiceImpl extends ServiceImpl<ForumBoardMapper, ForumBoard>
        implements IForumBoardService {

    private final ForumPostMapper postMapper;

    private final ForumModeratorMapper moderatorMapper;

    /**
     * 查询版块列表
     * */
    @Override
    public PageResult<ForumBoard> selectForumBoardList(ForumBoard forumBoard) {

        LambdaQueryWrapper<ForumBoard> queryWrapper = new LambdaQueryWrapper<>();

        if (forumBoard.getStatus() != null) {
            queryWrapper.eq(ForumBoard::getStatus, forumBoard.getStatus());
        }

        Page<ForumBoard> page = this.page(new Page<>(forumBoard.getPageNum(), forumBoard.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 添加版块
     * */
    @Override
    public void addForumBoard(ForumBoard forumBoard) {
        this.save(forumBoard);
    }

    /**
     * 更新版块
     * */
    @Override
    public void updateForumBoard(ForumBoard forumBoard) {
        Long boardId = forumBoard.getBoardId();
        if (boardId == null) {
            throw new ServiceException("版块ID不能为空");
        }

        ForumBoard existBoard = getById(boardId);
        if (existBoard == null) {
            throw new ServiceException("版块不存在");
        }

        // 如果更新了父版块，校验不能形成循环依赖
        Long newParentId = forumBoard.getParentId();
        if (newParentId != null && !Objects.equals(newParentId, existBoard.getParentId())) {
            if (Objects.equals(newParentId, boardId)) {
                throw new ServiceException("父版块不能为自身");
            }
            if (isChildBoard(boardId, newParentId)) {
                throw new ServiceException("不能将版块移动到其子版块下");
            }
        }

        // 校验同一父版块下名称是否重复
        String name = forumBoard.getName();
        if (StringUtils.isNotBlank(name)) {
            Long parentId = newParentId != null ? newParentId : existBoard.getParentId();
            LambdaQueryWrapper<ForumBoard> nameWrapper = new LambdaQueryWrapper<>();
            nameWrapper.eq(ForumBoard::getName, name)
                    .eq(ForumBoard::getParentId, parentId != null ? parentId : 0L)
                    .ne(ForumBoard::getBoardId, boardId);
            if (count(nameWrapper) > 0) {
                throw new ServiceException("同一父版块下已存在相同名称的版块");
            }
        }

        // 校验 slug 全局唯一
        String slug = forumBoard.getSlug();
        if (StringUtils.isNotBlank(slug)) {
            LambdaQueryWrapper<ForumBoard> slugWrapper = new LambdaQueryWrapper<>();
            slugWrapper.eq(ForumBoard::getSlug, slug)
                    .ne(ForumBoard::getBoardId, boardId);
            if (count(slugWrapper) > 0) {
                throw new ServiceException("版块别名已存在");
            }
        }

        // 只更新非空字段
        LambdaUpdateWrapper<ForumBoard> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ForumBoard::getBoardId, boardId);

        if (StringUtils.isNotBlank(name)) {
            updateWrapper.set(ForumBoard::getName, name);
        }
        if (StringUtils.isNotBlank(slug)) {
            updateWrapper.set(ForumBoard::getSlug, slug);
        }
        if (StringUtils.isNotBlank(forumBoard.getIcon())) {
            updateWrapper.set(ForumBoard::getIcon, forumBoard.getIcon());
        }
        if (StringUtils.isNotBlank(forumBoard.getDescription())) {
            updateWrapper.set(ForumBoard::getDescription, forumBoard.getDescription());
        }
        if (forumBoard.getSortOrder() != null) {
            updateWrapper.set(ForumBoard::getSortOrder, forumBoard.getSortOrder());
        }
        if (forumBoard.getStatus() != null) {
            updateWrapper.set(ForumBoard::getStatus, forumBoard.getStatus());
        }
        if (newParentId != null) {
            updateWrapper.set(ForumBoard::getParentId, newParentId);
        }
        updateWrapper.set(ForumBoard::getUpdateTime, LocalDateTime.now());

        update(updateWrapper);
    }

    /**
     * 删除版块
     * */
    @Override
    public void deleteForumBoard(Long boardId) {
        if (boardId == null) {
            throw new ServiceException("版块ID不能为空");
        }

        ForumBoard existBoard = getById(boardId);
        if (existBoard == null) {
            throw new ServiceException("版块不存在");
        }

        // 校验是否存在子版块
        LambdaQueryWrapper<ForumBoard> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(ForumBoard::getParentId, boardId);
        if (count(childWrapper) > 0) {
            throw new ServiceException("该版块下存在子版块，无法删除");
        }

        // 校验该版块下是否存在帖子
        LambdaQueryWrapper<ForumPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(ForumPost::getBoardId, boardId);
        if (postMapper.selectCount(postWrapper) > 0) {
            throw new ServiceException("该版块下存在帖子，无法删除");
        }

        // 删除版主关联
        LambdaQueryWrapper<ForumModerator> moderatorWrapper = new LambdaQueryWrapper<>();
        moderatorWrapper.eq(ForumModerator::getBoardId, boardId);
        moderatorMapper.delete(moderatorWrapper);

        // 删除版块
        removeById(boardId);
    }

    /**
     * 判断 targetId 是否为 boardId 的子版块（避免循环依赖）
     */
    private boolean isChildBoard(Long boardId, Long targetId) {
        if (targetId == null || targetId == 0L) {
            return false;
        }
        LambdaQueryWrapper<ForumBoard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumBoard::getParentId, boardId);
        List<ForumBoard> children = list(wrapper);
        for (ForumBoard child : children) {
            if (Objects.equals(child.getBoardId(), targetId)) {
                return true;
            }
            if (isChildBoard(child.getBoardId(), targetId)) {
                return true;
            }
        }
        return false;
    }
}
