package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.system.mapper.NotificationMapper;
import com.xy.system.service.INotificationService;
import com.xy.common.entity.Notification;
import com.xy.common.enums.NotificationType;
import com.xy.system.service.wk.WKClientService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知 逻辑层实现
 * */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements INotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final WKClientService wkClientService;

    @Override
    public Notification notifyUser(Long uId, NotificationType type, String title, String content,
                                   Long relatedId, String extra) {
        Notification notification = new Notification();
        notification.setUId(uId);
        notification.setType(type.getCode());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setExtra(extra);
        LocalDateTime now = LocalDateTime.now();
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        save(notification);

        // 通过悟空 IM 发送系统消息，实现实时推送
        try {
            wkClientService.sendSystemNotice(uId, content);
        } catch (Exception e) {
            LOGGER.error("发送IM通知失败, uId={}, type={}", uId, type, e);
        }
        return notification;
    }

    @Override
    public List<Notification> listUnread(Long uId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUId, uId)
                .eq(Notification::getIsRead, 0)
                .orderByDesc(Notification::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<Notification> listNotifications(Long uId, Integer type, Integer isRead,
                                                Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUId, uId);
        if (type != null) {
            wrapper.eq(Notification::getType, type);
        }
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        wrapper.orderByDesc(Notification::getCreateTime);

        if (pageNum != null && pageSize != null && pageSize > 0) {
            return page(new Page<>(pageNum, pageSize), wrapper)
                    .getRecords();
        }
        return list(wrapper);
    }

    @Override
    public boolean markRead(Long uId, Long notificationId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
                .eq(Notification::getUId, uId);
        Notification update = new Notification();
        update.setIsRead(1);
        update.setUpdateTime(LocalDateTime.now());
        return update(update, wrapper);
    }

    @Override
    public boolean markAllRead(Long uId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUId, uId)
                .eq(Notification::getIsRead, 0);
        Notification update = new Notification();
        update.setIsRead(1);
        update.setUpdateTime(LocalDateTime.now());
        return update(update, wrapper);
    }

    @Override
    public long countUnread(Long uId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUId, uId)
                .eq(Notification::getIsRead, 0);
        return count(wrapper);
    }
}
