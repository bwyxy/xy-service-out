package com.xy.app.controller;

import com.xy.app.pojo.dto.TodoFocusFinishDTO;
import com.xy.app.pojo.dto.TodoFocusStartDTO;
import com.xy.app.pojo.vo.TodoFocusTodayVO;
import com.xy.app.service.IFocusService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端番茄专注
 * */
@RestController
@RequestMapping("/focus")
@RequiredArgsConstructor
public class FocusController {

    private final IFocusService focusService;

    /**
     * 今日专注统计（番茄数、累计分钟、昨日番茄数）
     * */
    @GetMapping("/today")
    public R<TodoFocusTodayVO> today() {
        return R.ok(focusService.today());
    }

    /**
     * 开始一轮专注（clientId 幂等）
     * */
    @PostMapping("/session/start")
    public R<Void> start(@RequestBody TodoFocusStartDTO dto) {
        focusService.start(dto);

        return R.ok();
    }

    /**
     * 结束一轮专注：跑满一轮传 finished，中途放弃传 aborted，同一 clientId 重复调用只结算一次
     * */
    @PostMapping("/session/finish")
    public R<Void> finish(@RequestBody TodoFocusFinishDTO dto) {
        focusService.finish(dto);

        return R.ok();
    }
}
