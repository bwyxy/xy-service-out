package com.xy.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.app.mapper.TodoTaskCollaboratorMapper;
import com.xy.app.service.ITodoTaskCollaboratorService;
import com.xy.common.entity.TodoTaskCollaborator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TodoTaskCollaboratorServiceImpl extends ServiceImpl<TodoTaskCollaboratorMapper, TodoTaskCollaborator> implements ITodoTaskCollaboratorService {

    /**
     * 加入任务协作
     * */
    public void joinTaskCollaborator(Long userId, Long taskId, String isAdmin) {

        if (userId == null) {
            throw new SecurityException("用户 Id不能为空");
        }

        if (taskId == null) {
            throw new SecurityException("任务 Id不能为空");
        }

        TodoTaskCollaborator collaborator = new TodoTaskCollaborator();

        collaborator.setUserId(userId);
        collaborator.setTaskId(taskId);
        collaborator.setIsAdmin(isAdmin);
        collaborator.setJoinedAt(LocalDateTime.now());
        collaborator.setCreateTime(LocalDateTime.now());
        collaborator.setUpdateTime(LocalDateTime.now());

        save(collaborator);
    }
}
