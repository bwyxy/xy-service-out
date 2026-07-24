package com.xy.app.service;

import java.util.List;

public interface ITodoTaskTagService {

    /**
     * 添加任务和标签的关联
     * */
    public void addTaskTags(Long taskId, List<Long> tagIds);

    /**
     * 更新添加任务和标签的关联
     * */
    public void updateTaskTags(Long taskId, List<Long> tagIds);
}
