package com.chat.fg.service;

import com.chat.fg.pojo.dto.ConversationDTO;
import com.chat.fg.pojo.dto.ConversationTopDTO;
import com.chat.fg.pojo.vo.UserConversationVO;

import java.util.List;

/**
 * 会话 逻辑层
 * */
public interface IConversationService {

    /**
     * 获取会话列表
     * */
    public List<UserConversationVO> getConversations(ConversationDTO conversationDTO);

    /**
     * 设置/取消会话置顶
     * */
    public void updateTop(ConversationTopDTO conversationTopDTO);
}
