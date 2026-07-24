package com.xy.blog.controller;

import com.xy.blog.pojo.dto.CommentDTO;
import com.xy.blog.service.ICommentService;
import com.xy.common.core.domain.R;
import com.xy.common.core.domain.model.LoginUser;
import com.xy.common.entity.SysComment;
import com.xy.common.ip.AddressUtils;
import com.xy.common.ip.IpUtils;
import com.xy.common.utils.SecurityUtils;
import com.xy.common.utils.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/web/comment")
@RequiredArgsConstructor
public class CommentController {

    @Autowired
    private ICommentService commentService;

    /**
     * 评论
     * */
    @PostMapping("/add")
    public R<?> addComment(
            @RequestBody SysComment sysComment,
            @AuthenticationPrincipal LoginUser loginUser,
            HttpServletRequest request
    ) {

        Long userId = null;

        if (loginUser != null) {
            userId = SecurityUtils.getUserId();
        }

        String ip = IpUtils.getIpAddr(request);
        String ipAddress = AddressUtils.getRealAddressByIP(ip);

        String deviceInfo = sysComment.getDeviceInfo();
        if (StringUtils.isBlank(deviceInfo)) {
            deviceInfo = request.getHeader("User-Agent");
        }

        sysComment.setCommentIp(ip);
        sysComment.setIpAddress(ipAddress);
        sysComment.setDeviceInfo(deviceInfo);

        SysComment comment = commentService.addComment(sysComment, userId);

        return R.ok(comment);
    }

    /**
     * 获取评论列表树
     * */
    @PostMapping("/tree")
    public R<?> getCommentTree(@RequestBody CommentDTO commentDTO) {
        List<SysComment> comments = commentService.getCommentTree(commentDTO);

        return R.ok(comments);
    }

    /**
     * 获取评论列表
     * */
    @PostMapping("/list")
    public R<?> list(@RequestBody CommentDTO commentDTO) {

        List<SysComment> list = commentService.getCommentList(commentDTO);

        return R.ok(list);
    }
}
