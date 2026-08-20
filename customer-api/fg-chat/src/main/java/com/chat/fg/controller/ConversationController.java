package com.chat.fg.controller;

import com.chat.fg.pojo.dto.ConversationDTO;
import com.chat.fg.pojo.vo.UserConversationVO;
import com.chat.fg.service.IConversationService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话
 * */
@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
public class ConversationController {

    @Autowired
    private IConversationService conversationService;

    /**
     * 获取会话列表
     */
    @PostMapping("/list")
    public R<?> list(ConversationDTO conversationDTO) {

        List<UserConversationVO> conversations = conversationService.getConversations(conversationDTO);

        return R.ok(conversations);
    }
}
