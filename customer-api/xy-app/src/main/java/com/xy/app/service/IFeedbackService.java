package com.xy.app.service;

import com.xy.app.pojo.dto.TodoFeedbackSaveDTO;

/**
 * 意见反馈
 * */
public interface IFeedbackService {

    /**
     * 提交一条意见反馈
     * */
    void submit(TodoFeedbackSaveDTO dto);
}
