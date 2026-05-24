package com.todo.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.todo.common.entity.SysTag;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysTagMapper extends BaseMapper<SysTag> {

    /**
     * 获取标签列表(统计关联文章数)
     * */
    public List<SysTag> getSysTagList();
}
