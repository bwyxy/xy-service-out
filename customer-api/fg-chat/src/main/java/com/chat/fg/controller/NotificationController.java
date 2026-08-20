package com.chat.fg.controller;

import com.chat.fg.pojo.vo.NotificationVO;
import com.xy.system.service.INotificationService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.core.domain.R;
import com.xy.common.entity.Notification;
import com.xy.common.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知 控制层
 * */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    /**
     * 查询通知列表
     * */
    @GetMapping("/list")
    public R<List<NotificationVO>> list(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        Long currentUid = SecurityUtils.getUId();
        List<Notification> list = notificationService.listNotifications(currentUid, type, isRead, pageNum, pageSize);
        List<NotificationVO> result = list.stream().map(this::convert).toList();
        return R.ok(result);
    }

    /**
     * 未读通知列表
     * */
    @GetMapping("/unread")
    public R<List<NotificationVO>> unreadList() {
        Long currentUid = SecurityUtils.getUId();
        List<Notification> list = notificationService.listUnread(currentUid);
        List<NotificationVO> result = list.stream().map(this::convert).toList();
        return R.ok(result);
    }

    /**
     * 未读通知数量
     * */
    @GetMapping("/unread/count")
    public R<Long> unreadCount() {
        Long currentUid = SecurityUtils.getUId();
        return R.ok(notificationService.countUnread(currentUid));
    }

    /**
     * 标记单条已读
     * */
    @PostMapping("/read/{notificationId}")
    public R<?> markRead(@PathVariable Long notificationId) {
        Long currentUid = SecurityUtils.getUId();
        notificationService.markRead(currentUid, notificationId);
        return R.ok("标记成功");
    }

    /**
     * 标记全部已读
     * */
    @PostMapping("/read/all")
    public R<?> markAllRead() {
        Long currentUid = SecurityUtils.getUId();
        notificationService.markAllRead(currentUid);
        return R.ok("标记成功");
    }

    private NotificationVO convert(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setType(n.getType());
        NotificationType typeEnum = NotificationType.of(n.getType());
        vo.setTypeDesc(typeEnum == null ? "" : typeEnum.getDesc());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setRelatedId(n.getRelatedId());
        vo.setIsRead(n.getIsRead());
        vo.setExtra(n.getExtra());
        vo.setCreateTime(n.getCreateTime());
        return vo;
    }
}
