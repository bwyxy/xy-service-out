package com.todo.blog.service;

import com.todo.blog.pojo.dto.CommentDTO;
import com.todo.common.entity.SysComment;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 评论 逻辑层
 * */
public interface ICommentService {

    /**
     * 发表评论或回复
     * */
    public SysComment addComment(SysComment sysComment, Long currentUserId);

    /**
     * 获取评论树（嵌套结构
     * */
    public List<SysComment> getCommentTree(CommentDTO commentDTO);

    /**
     * 获取评论列表
     * */
    public List<SysComment> getCommentList(CommentDTO commentDTO);
}
