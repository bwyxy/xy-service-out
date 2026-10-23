package com.xy.app.service.support;

import com.xy.app.pojo.vo.TodoTaskCreateVO;
import com.xy.app.pojo.vo.TodoTaskDetailVO;
import com.xy.app.pojo.vo.TodoTaskVO;
import com.xy.common.entity.todo.TodoTask;

/**
 * 待办任务实体转前端视图对象（无状态）
 * */
public final class TodoTaskConverter {

    private TodoTaskConverter() {
    }

    /**
     * 创建任务返回：只回传新任务的 Id、提醒时间与同步序号
     * */
    public static TodoTaskCreateVO toCreateVO(TodoTask task) {
        TodoTaskCreateVO vo = new TodoTaskCreateVO();
        vo.setTaskId(task.getTaskId());
        vo.setRemindAt(task.getRemindAt());
        vo.setSeq(task.getSeq());

        return vo;
    }

    /**
     * 列表与象限预览用的任务视图
     * */
    public static TodoTaskVO toListVO(TodoTask task) {
        TodoTaskVO vo = new TodoTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setTitle(task.getTaskTitle());
        vo.setDescription(task.getDescription());
        vo.setTagId(task.getTagId());
        vo.setPriority(task.getPriority());
        vo.setUrgencyLevel(task.getUrgencyLevel());
        vo.setCompleted(task.getCompleted() == null ? 0 : task.getCompleted().intValue());
        vo.setCompletionTime(task.getCompletionTime());
        vo.setArchived(task.getArchived() == null ? 0 : task.getArchived());
        vo.setArchiveTime(task.getArchiveTime());
        vo.setDueDate(task.getDueDate());
        vo.setRepeatRule(task.getRepeatRule());
        vo.setRemindBefore(task.getRemindBefore());
        vo.setSeq(task.getSeq());

        return vo;
    }

    /**
     * 详情页用的任务视图（清单名由调用方另查，这里只搬实体上的字段）
     * */
    public static TodoTaskDetailVO toDetailVO(TodoTask task) {
        TodoTaskDetailVO vo = new TodoTaskDetailVO();
        vo.setTaskId(task.getTaskId());
        vo.setTitle(task.getTaskTitle());
        vo.setDescription(task.getDescription());
        vo.setTagId(task.getTagId());
        vo.setPriority(task.getPriority());
        vo.setUrgencyLevel(task.getUrgencyLevel());
        vo.setCompleted(task.getCompleted() == null ? 0 : task.getCompleted().intValue());
        vo.setCompletionTime(task.getCompletionTime());
        vo.setArchived(task.getArchived() == null ? 0 : task.getArchived());
        vo.setArchiveTime(task.getArchiveTime());
        vo.setDueDate(task.getDueDate());
        vo.setRepeatRule(task.getRepeatRule());
        vo.setRemindBefore(task.getRemindBefore());
        vo.setRemindAt(task.getRemindAt());
        vo.setCreateTime(task.getCreateTime());

        return vo;
    }
}
