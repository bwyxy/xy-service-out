package com.todo.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.todo.blog.pojo.dto.CommentDTO;
import com.todo.blog.service.ICommentService;
import com.todo.common.entity.SysComment;
import com.todo.common.enums.CommentType;
import com.todo.common.ip.AddressUtils;
import com.todo.common.ip.IpUtils;
import com.todo.common.utils.StringUtils;
import com.todo.system.mapper.SysCommentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommentServiceImpl extends ServiceImpl<SysCommentMapper, SysComment> implements ICommentService {

    /**
     * 发表评论或回复
     * */
    @Override
    public SysComment addComment(SysComment sysComment, Long currentUserId) {
        CommentType type = CommentType.fromCode(sysComment.getCommentType());

        if (type == CommentType.ARTICLE && sysComment.getArticleId() == null) {
            throw new SecurityException("文章 Id不能为空");
        }

        if (sysComment.getParentId() == 0) {
            sysComment.setParentId(null);
        }

        if (sysComment.getAvatarEmoji() == null) {
            sysComment.setAvatarEmoji("\uD83D\uDC64");
        }

        LambdaQueryWrapper<SysComment> queryWrapper = new LambdaQueryWrapper<>();

        if (sysComment.getParentId() == null) {
            sysComment.setRootId(null);
            sysComment.setLevel(0);
        } else {
            queryWrapper.eq(SysComment::getId, sysComment.getParentId());

            SysComment parentComment = this.getOne(queryWrapper);

            sysComment.setRootId(parentComment.getRootId() == null ? parentComment.getId() : parentComment.getRootId());

            sysComment.setLevel(parentComment.getLevel() + 1);
        }

        save(sysComment);

        return sysComment;
    }

    /**
     * 获取评论列表
     * */
    @Override
    public List<SysComment> getCommentList(CommentDTO commentDTO) {
        CommentType type = verificationCommentParams(commentDTO);

        LambdaQueryWrapper<SysComment> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysComment::getCommentType, type)
                .orderByDesc(SysComment::getCreateTime);

        return list(queryWrapper);
    }

    /**
     * 获取评论树（嵌套结构
     * */
    public List<SysComment> getCommentTree(CommentDTO commentDTO) {
        CommentType type = verificationCommentParams(commentDTO);

        // 1. 查出所有根评论
        LambdaQueryWrapper<SysComment> queryWrapper = new LambdaQueryWrapper<>();

        if (type == CommentType.ARTICLE) {
            queryWrapper.eq(SysComment::getArticleId, commentDTO.getArticleId());
        }
        queryWrapper.eq(SysComment::getCommentType, commentDTO.getCommentType())
                .eq(SysComment::getStatus, 1)
                .isNull(SysComment::getParentId)
                .orderByDesc(SysComment::getCreateTime);

        List<SysComment> rootComments = list(queryWrapper);

        if (rootComments.isEmpty()) return new ArrayList<>();

        // 2. 查出所有非根评论
        LambdaQueryWrapper<SysComment> replyWrapper = new LambdaQueryWrapper<>();

        if (type == CommentType.ARTICLE) {
            replyWrapper.eq(SysComment::getArticleId, commentDTO.getArticleId());
        }

        replyWrapper.eq(SysComment::getCommentType, commentDTO.getCommentType())
                .isNotNull(SysComment::getParentId)
                .eq(SysComment::getStatus, 1)
                .orderByDesc(SysComment::getCreateTime);

        List<SysComment> allReplies = list(replyWrapper);

        // 3. 构建父子映射
        Map<Long, List<SysComment>> parentIdMap = allReplies.stream()
                .collect(Collectors.groupingBy(SysComment::getParentId));

        // 4. 递归填充 children
        rootComments.forEach(root -> buildChildren(root, parentIdMap));

        return rootComments;
    }

    private void buildChildren(SysComment parent, Map<Long, List<SysComment>> parentMap) {
        List<SysComment> children = parentMap.getOrDefault(parent.getId(), new ArrayList<>());

        parent.setChildren(children);

        for (SysComment child : children) {
            buildChildren(child, parentMap);
        }
    }

    /**
     * 校验评论参数
     * */
    private CommentType verificationCommentParams(CommentDTO commentDTO) {
        if (StringUtils.isEmpty(commentDTO.getCommentType())) {
            throw new SecurityException("查询评论类型不能为空");
        }

        CommentType type = CommentType.fromCode(commentDTO.getCommentType());

        if (type == CommentType.ARTICLE && commentDTO.getArticleId() == null) {
            throw new SecurityException("查询文章 Id不能为空");
        }

        return type;
    }
}
