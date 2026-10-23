package com.xy.app.controller;

import com.xy.app.pojo.dto.TodoSubscribeReportDTO;
import com.xy.app.pojo.vo.TodoNotifyQuotaVO;
import com.xy.app.service.INotifyService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信订阅消息额度
 * */
@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final INotifyService notifyService;

    /**
     * 上报订阅授权结果，累加可下发条数
     * */
    @PostMapping("/subscribe")
    public R<TodoNotifyQuotaVO> subscribe(@RequestBody TodoSubscribeReportDTO dto) {
        return R.ok(notifyService.reportSubscribe(dto));
    }

    /**
     * 查当前用户剩余可下发条数
     * */
    @GetMapping("/quota")
    public R<TodoNotifyQuotaVO> quota() {
        return R.ok(notifyService.getQuota());
    }
}
