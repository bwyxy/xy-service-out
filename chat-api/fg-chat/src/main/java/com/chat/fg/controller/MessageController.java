package com.chat.fg.controller;

import com.chat.fg.pojo.dto.MsgDTO;
import com.chat.fg.pojo.dto.SendMsgDTO;
import com.chat.fg.service.IMessageService;
import com.xy.common.client.vo.MessageVO;
import com.xy.common.core.domain.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息 控制层
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
    public R<?> getMessages(@Valid MsgDTO msgDTO) {
        return R.ok(messageService.getMessageSync(msgDTO));
    }

    /**
     * 发送消息（经服务端转发至悟空 IM）
     *
     * <p>前端不再直接调用悟空 IM SDK 发送消息，而是通过此接口由服务端统一转发，
     * 便于后续做内容安全校验、敏感词过滤、图片鉴黄等业务扩展。</p>
     * */
    @PostMapping("/send")
    public R<?> sendMessage(@Valid @RequestBody SendMsgDTO sendMsgDTO) {
        return R.ok(messageService.sendMessage(sendMsgDTO));
    }
}
