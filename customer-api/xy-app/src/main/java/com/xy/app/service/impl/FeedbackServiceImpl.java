package com.xy.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.app.mapper.FeedbackMapper;
import com.xy.app.pojo.dto.TodoFeedbackSaveDTO;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.IFeedbackService;
import com.xy.common.entity.todo.TodoFeedback;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 意见反馈服务实现
 * */
@Slf4j
@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, TodoFeedback> implements IFeedbackService {

    /**
     * 反馈内容长度上限，与 t_todo_feedback.content varchar(1000) 对齐
     * */
    private static final int CONTENT_MAX_LENGTH = 500;

    /**
     * 联系方式长度上限，与 t_todo_feedback.contact varchar(100) 对齐
     * */
    private static final int CONTACT_MAX_LENGTH = 100;

    /**
     * 待处理
     * */
    private static final int STATUS_PENDING = 0;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(TodoFeedbackSaveDTO dto) {

        String content = dto.getContent() == null ? "" : dto.getContent().trim();

        if (content.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "请先写点想说的话");
        }

        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR,
                    "反馈内容不能超过" + CONTENT_MAX_LENGTH + "个字");
        }

        String contact = dto.getContact() == null ? "" : dto.getContact().trim();

        if (contact.length() > CONTACT_MAX_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR,
                    "联系方式不能超过" + CONTACT_MAX_LENGTH + "个字");
        }

        TodoFeedback feedback = new TodoFeedback();
        feedback.setUserId(TodoUserDetails.getCurrentUId());
        feedback.setContent(content);
        feedback.setContact(contact);
        feedback.setImages(joinImages(dto.getImages()));
        feedback.setStatus(STATUS_PENDING);
        feedback.setCreateTime(LocalDateTime.now());

        save(feedback);
    }

    /**
     * 配图地址拼成逗号分隔，与表中 images 的存储约定一致
     * */
    private String joinImages(List<String> images) {

        if (images == null || images.isEmpty()) {
            return "";
        }

        return images.stream()
                .filter(image -> image != null && !image.isBlank())
                .collect(Collectors.joining(","));
    }
}
