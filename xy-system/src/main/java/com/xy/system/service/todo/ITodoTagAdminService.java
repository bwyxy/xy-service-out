package com.xy.system.service.todo;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.TodoTag;

public interface ITodoTagAdminService {

    /**
     * 分页查询标签列表
     */
    PageResult<TodoTag> selectTagList(TodoTag tag);

    /**
     * 根据标签ID查询详情
     */
    TodoTag selectTagById(Long tagId);

    /**
     * 新增标签
     */
    Long addTag(TodoTag tag);

    /**
     * 修改标签
     */
    boolean updateTag(TodoTag tag);

    /**
     * 删除标签
     */
    boolean deleteTag(Long tagId);
}
