package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户意见反馈
 * */
@Mapper
public interface FeedbackMapper extends BaseMapper<TodoFeedback> {
}
