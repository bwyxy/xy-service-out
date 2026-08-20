package com.chat.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.ai.dto.ConversationQuery;
import com.chat.ai.entity.AiConversation;
import com.chat.ai.entity.AiMessage;
import com.chat.ai.enums.ConversationStatus;
import com.chat.ai.enums.MessageRole;
import com.chat.ai.mapper.AiConversationMapper;
import com.chat.ai.service.AiConversationService;
import com.chat.ai.service.AiMessageService;
import com.chat.ai.vo.ConversationVo;
import com.chat.ai.vo.MessageVo;
import com.xy.common.core.domain.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 会话服务实现
 */
@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {

    private final AiMessageService messageService;

    public AiConversationServiceImpl(AiMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public AiConversation createConversation(Long userId, String userType, String title,
                                             String providerCode, String model, String systemPrompt) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setUserType(userType);
        conversation.setTitle(title != null && !title.isEmpty() ? title : "新对话");
        conversation.setProviderCode(providerCode);
        conversation.setModel(model);
        conversation.setSystemPrompt(systemPrompt);
        conversation.setStatus(ConversationStatus.ACTIVE.getCode());
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        save(conversation);
        return conversation;
    }

    @Override
    public PageResult<ConversationVo> pageConversations(ConversationQuery query, int pageNum, int pageSize) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getUserId() != null, AiConversation::getUserId, query.getUserId())
                .eq(query.getUserType() != null, AiConversation::getUserType, query.getUserType())
                .eq(query.getStatus() != null, AiConversation::getStatus, query.getStatus())
                .like(query.getKeyword() != null && !query.getKeyword().isEmpty(),
                        AiConversation::getTitle, query.getKeyword())
                .orderByDesc(AiConversation::getUpdateTime);
        Page<AiConversation> page = page(new Page<>(pageNum, pageSize), wrapper);
        List<ConversationVo> records = page.getRecords().stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), (long) pageNum, (long) pageSize);
    }

    @Override
    public List<MessageVo> listMessages(Long conversationId) {
        List<AiMessage> messages = messageService.listByConversation(conversationId);
        return messages.stream()
                .map(this::convertToMessageVo)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        AiConversation conversation = getById(id);
        if (conversation == null) {
            return false;
        }
        conversation.setStatus(status);
        conversation.setUpdateTime(LocalDateTime.now());
        return updateById(conversation);
    }

    private ConversationVo convertToVo(AiConversation conversation) {
        ConversationVo vo = new ConversationVo();
        BeanUtils.copyProperties(conversation, vo);
        return vo;
    }

    private MessageVo convertToMessageVo(AiMessage message) {
        MessageVo vo = new MessageVo();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}
