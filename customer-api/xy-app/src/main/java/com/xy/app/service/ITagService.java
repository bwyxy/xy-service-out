package com.xy.app.service;

import com.xy.app.pojo.dto.TodoTagSaveDTO;
import com.xy.app.pojo.vo.TodoTagVO;

import java.util.List;

/**
 * 客户端清单服务
 * */
public interface ITagService {

    /**
     * 清单列表（含各清单待完成数）
     * */
    List<TodoTagVO> listTags();

    /**
     * 新增清单
     * */
    TodoTagVO createTag(TodoTagSaveDTO dto);

    /**
     * 修改清单
     * */
    void updateTag(Long tagId, TodoTagSaveDTO dto);

    /**
     * 删除清单（清单下的任务改为未归类，不跟着删）
     * */
    void removeTag(Long tagId);

    /**
     * 初始化默认清单（首次登录调用，幂等：已有默认清单则跳过）
     * */
    void initDefaultTags(Long uId);
}
