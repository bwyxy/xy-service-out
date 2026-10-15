package com.xy.system.service.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.dto.blog.DispatchRequestDTO;
import com.xy.common.entity.blog.BlogDispatchTask;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.common.vo.blog.DispatchSubmitVO;
import com.xy.common.vo.blog.DispatchTaskVO;

import java.util.List;

/**
 * 分发任务服务
 * */
public interface IBlogDispatchTaskService {

    /**
     * 提交分发：支持单篇/多篇与单平台/多平台组合，任务落库后由异步执行器处理
     * */
    DispatchSubmitVO submitDispatch(DispatchRequestDTO request);

    /**
     * 分页查询分发任务
     * */
    PageResult<DispatchTaskVO> selectTaskPage(BlogDispatchTask query);

    /**
     * 查询任务详情
     * */
    DispatchTaskVO selectTaskById(Long id);

    /**
     * 重试任务（单个或批量）
     * */
    void retryTasks(List<Long> taskIds);

    /**
     * 删除任务（单个或批量），执行中的任务不允许删除
     * */
    void deleteTasks(List<Long> taskIds);

    /**
     * 查询待分发任务 Id
     * */
    List<Long> selectPendingTaskIds(int limit);

    /**
     * 查询可自动重试的失败任务 Id
     * */
    List<Long> selectRetryableTaskIds(int limit);

    /**
     * 标记任务为分发中，返回 false 表示任务已被其他线程抢占
     * */
    boolean markRunning(Long taskId);

    /**
     * 标记任务分发成功
     * */
    void markSuccess(Long taskId, String platformArticleId, String resultUrl, long durationMs);

    /**
     * 标记任务分发失败
     * */
    void markFailed(Long taskId, DispatchErrorTypeEnum errorType, String errorMsg, long durationMs);
}
