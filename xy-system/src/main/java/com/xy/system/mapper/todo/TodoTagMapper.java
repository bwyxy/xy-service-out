package com.xy.system.mapper.todo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.TodoTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TodoTagMapper extends BaseMapper<TodoTag> {

    /**
     * 分页查询标签列表（关联用户名）
     */
    List<TodoTag> selectAdminTagList(@Param("tag") TodoTag tag);

    /**
     * 统计标签总数
     */
    Long countAdminTagList(@Param("tag") TodoTag tag);

    /**
     * 根据标签ID查询
     */
    TodoTag selectAdminTagById(@Param("tagId") Long tagId);

    /**
     * 管理后台新增标签
     */
    int insertAdminTag(@Param("tag") TodoTag tag);

    /**
     * 管理后台修改标签
     */
    int updateAdminTag(@Param("tag") TodoTag tag);

    /**
     * 管理后台删除标签
     */
    int deleteAdminTag(@Param("tagId") Long tagId);
}
