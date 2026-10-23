package com.xy.app.controller;

import com.xy.app.pojo.dto.TodoFeedbackSaveDTO;
import com.xy.app.service.IFeedbackService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 意见反馈
 * */
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final IFeedbackService feedbackService;

    /**
     * 提交意见反馈
     * */
    @PostMapping
    public R<Void> submit(@RequestBody TodoFeedbackSaveDTO dto) {
        feedbackService.submit(dto);

        return R.ok();
    }
}
