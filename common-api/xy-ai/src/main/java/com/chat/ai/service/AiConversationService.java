package com.chat.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.ai.dto.ConversationQuery;
import com.chat.ai.entity.AiConversation;
import com.chat.ai.vo.ConversationVo;
import com.chat.ai.vo.MessageVo;
import com.xy.common.core.domain.PageResult;

import java.util.List;

/**
 * AI 会话服务
 */
public interface AiConversationService extends IService<AiConversation> {

    /**
     * 创建会话
     */
    AiConversation createConversation(AiConversation aiConversation);

    /**
     * 分页查询会话
     */
    PageResult<ConversationVo> pageConversations(ConversationQuery query, int pageNum, int pageSize);

    /**
     * 查询会话消息
     */
    List<MessageVo> listMessages(Long conversationId);

    /**
     * 归档/删除会话
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 获取用户最新的活跃会话
     */
    AiConversation getLatestActiveConversation(Long userId, String userType);
}
