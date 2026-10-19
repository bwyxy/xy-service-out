package com.chat.fg.controller;

import com.chat.fg.pojo.dto.ConversationChannelDTO;
import com.chat.fg.pojo.dto.ConversationDTO;
import com.chat.fg.pojo.dto.ConversationTopDTO;
import com.chat.fg.pojo.vo.UserConversationVO;
import com.chat.fg.service.IConversationService;
import com.xy.common.core.domain.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * 设置/取消会话置顶
     */
    @PostMapping("/top")
    public R<?> top(@Valid @RequestBody ConversationTopDTO conversationTopDTO) {

        conversationService.updateTop(conversationTopDTO);

        return R.ok();
    }

    /**
     * 删除聊天记录（仅当前用户不可见，对方不受影响）
     */
    @PostMapping("/clear")
    public R<?> clear(@Valid @RequestBody ConversationChannelDTO conversationChannelDTO) {

        conversationService.clearMessages(conversationChannelDTO);

        return R.ok();
    }

    /**
     * 删除会话（仅当前用户不可见，对方不受影响）
     */
    @PostMapping("/delete")
    public R<?> delete(@Valid @RequestBody ConversationChannelDTO conversationChannelDTO) {

        conversationService.deleteConversation(conversationChannelDTO);

        return R.ok();
    }
}
