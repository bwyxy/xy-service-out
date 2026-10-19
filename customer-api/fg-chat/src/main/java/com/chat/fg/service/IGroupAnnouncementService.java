package com.chat.fg.service;

import com.chat.fg.pojo.dto.PublishAnnouncementDTO;
import com.xy.common.entity.chat.GroupAnnouncement;

import java.util.List;

/**
 * 群公告 逻辑层
 * */
public interface IGroupAnnouncementService {

    /**
     * 发布群公告
     * */
    void publishAnnouncement(PublishAnnouncementDTO publishAnnouncementDTO);

    /**
     * 查询群公告列表
     * */
    List<GroupAnnouncement> listAnnouncements(Long groupId);

    /**
     * 删除群公告
     * */
    void deleteAnnouncement(Long groupId, Long id);
}
