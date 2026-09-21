package com.xy.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.entity.SysCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysCategoryMapper extends BaseMapper<SysCategory> {

    /**
     * 分页查询分类列表（统计关联文章数量）
     * */
    List<SysCategory> selectSysCategoryList(Page<SysCategory> page, @Param("sysCategory") SysCategory sysCategory);
}
