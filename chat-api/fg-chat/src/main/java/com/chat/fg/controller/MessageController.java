package com.chat.fg.controller;

import com.chat.fg.pojo.dto.MsgDTO;
import com.chat.fg.service.IMessageService;
import com.xy.common.client.dto.request.MsgRequest;
import com.xy.common.client.vo.MessageVO;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 历史消息 控制层
 * */
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    @Autowired
    private IMessageService messageService;

    /**
     * 获取历史消息
     * */
    @PostMapping("/list")
    public R<?> getMessages(MsgDTO msgDTO) {

        return R.ok(messageService.getMessageSync(msgDTO));
    }
}
