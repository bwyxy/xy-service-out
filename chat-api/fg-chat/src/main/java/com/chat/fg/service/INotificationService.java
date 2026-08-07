package com.chat.fg.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.entity.Notification;
import com.xy.common.enums.NotificationType;

import java.util.List;

/**
 * 通知 逻辑层
 * */
public interface INotificationService extends IService<Notification> {

    /**
     * 创建并发送通知
     *
     * @param uId 接收者ID
     * @param type 通知类型
     * @param title 标题
     * @param content 内容
     * @param relatedId 关联业务ID
     * @param extra 扩展信息
     * @return 通知记录
     * */
    Notification notifyUser(Long uId, NotificationType type, String title, String content,
                            Long relatedId, String extra);

    /**
     * 查询未读通知列表
     * */
    List<Notification> listUnread(Long uId);

    /**
     * 分页查询通知列表
     * */
    List<Notification> listNotifications(Long uId, Integer type, Integer isRead, Integer pageNum, Integer pageSize);

    /**
     * 标记单条通知已读
     * */
    boolean markRead(Long uId, Long notificationId);

    /**
     * 标记全部已读
     * */
    boolean markAllRead(Long uId);

    /**
     * 统计未读通知数
     * */
    long countUnread(Long uId);
}
