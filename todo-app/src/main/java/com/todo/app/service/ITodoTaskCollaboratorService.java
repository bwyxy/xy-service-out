package com.todo.app.service;

/**
 * 任务协作 逻辑层
 * */
public interface ITodoTaskCollaboratorService {

    /**
     * 加入任务协作
     * */
    public void joinTaskCollaborator(Long userId, Long taskId, String isAdmin);
}
