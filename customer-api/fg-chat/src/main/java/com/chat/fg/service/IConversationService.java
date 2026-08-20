package com.chat.fg.service;

import com.chat.fg.pojo.dto.ConversationDTO;
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
}
