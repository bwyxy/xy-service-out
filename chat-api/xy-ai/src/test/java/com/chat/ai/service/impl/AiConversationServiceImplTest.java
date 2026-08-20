package com.chat.ai.service.impl;

import com.chat.ai.dto.ConversationQuery;
import com.chat.ai.entity.AiConversation;
import com.chat.ai.enums.ConversationStatus;
import com.chat.ai.service.AiConversationService;
import com.chat.ai.service.AiMessageService;
import com.chat.ai.vo.ConversationVo;
import com.chat.ai.vo.MessageVo;
import com.xy.common.core.domain.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-test.sql")
@Transactional
class AiConversationServiceImplTest {

    @Autowired
    private AiConversationService conversationService;

    @Autowired
    private AiMessageService messageService;

    @Test
    void shouldCreateConversation() {
        AiConversation conversation = conversationService.createConversation(1L, "CLIENT",
                "测试会话", "openai", "test-model", "你是助手");
        assertNotNull(conversation.getId());
        assertEquals("测试会话", conversation.getTitle());
        assertEquals(ConversationStatus.ACTIVE.getCode(), conversation.getStatus());
    }

    @Test
    void shouldPageConversations() {
        conversationService.createConversation(1L, "CLIENT", "对话 A", "openai", null, null);
        conversationService.createConversation(1L, "CLIENT", "对话 B", "openai", null, null);

        ConversationQuery query = new ConversationQuery();
        query.setUserId(1L);
        query.setUserType("CLIENT");
        PageResult<ConversationVo> page = conversationService.pageConversations(query, 1, 10);
        assertEquals(2, page.getList().size());
    }

    @Test
    void shouldListMessages() {
        AiConversation conversation = conversationService.createConversation(1L, "CLIENT",
                "测试", "openai", null, null);
        messageService.saveUserMessage(conversation.getId(), "你好");
        messageService.saveAssistantMessage(conversation.getId(), "test-model");

        List<MessageVo> messages = conversationService.listMessages(conversation.getId());
        assertEquals(2, messages.size());
    }

    @Test
    void shouldUpdateStatus() {
        AiConversation conversation = conversationService.createConversation(1L, "CLIENT",
                "测试", "openai", null, null);
        assertTrue(conversationService.updateStatus(conversation.getId(), ConversationStatus.ARCHIVED.getCode()));
        AiConversation updated = conversationService.getById(conversation.getId());
        assertEquals(ConversationStatus.ARCHIVED.getCode(), updated.getStatus());
    }
}
