package com.chat.ai.controller;

import com.chat.ai.dto.ConversationQuery;
import com.chat.ai.enums.ConversationStatus;
import com.chat.ai.service.AiConversationService;
import com.chat.ai.vo.ConversationVo;
import com.chat.ai.vo.MessageVo;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 会话管理接口
 */
@RestController
@RequestMapping("/ai/conversation")
public class AiConversationController {

    private final AiConversationService conversationService;

    public AiConversationController(AiConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/page")
    public R<PageResult<ConversationVo>> page(@RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) String userType,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        ConversationQuery query = new ConversationQuery();
        query.setUserId(userId);
        query.setUserType(userType);
        query.setKeyword(keyword);
        query.setStatus(status);
        return R.ok(conversationService.pageConversations(query, pageNum, pageSize));
    }

    @GetMapping("/{id}/messages")
    public R<List<MessageVo>> messages(@PathVariable Long id) {
        return R.ok(conversationService.listMessages(id));
    }

    @PutMapping("/{id}/archive")
    public R<Boolean> archive(@PathVariable Long id) {
        return R.ok(conversationService.updateStatus(id, ConversationStatus.ARCHIVED.getCode()));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(conversationService.updateStatus(id, ConversationStatus.DELETED.getCode()));
    }
}
