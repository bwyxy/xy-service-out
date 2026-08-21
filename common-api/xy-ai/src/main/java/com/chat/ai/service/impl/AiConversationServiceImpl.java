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
    public AiConversation createConversation(AiConversation aiConversation) {
        AiConversation conversation = new AiConversation();

        String title = aiConversation.getTitle();

        if (aiConversation.getConversationId() != null) {
            conversation.setConversationId(aiConversation.getConversationId());
        }

        conversation.setUserId(aiConversation.getUserId());
        conversation.setUserType(aiConversation.getUserType());
        conversation.setTitle(title != null && !title.isEmpty() ? title : "新对话");
        conversation.setProviderCode(aiConversation.getProviderCode());
        conversation.setModel(aiConversation.getModel());
        conversation.setSystemPrompt(aiConversation.getSystemPrompt());
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

    @Override
    public AiConversation getLatestActiveConversation(Long userId, String userType) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getUserType, userType)
                .eq(AiConversation::getStatus, ConversationStatus.ACTIVE.getCode())
                .orderByDesc(AiConversation::getUpdateTime)
                .last("limit 1");
        return getOne(wrapper);
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
