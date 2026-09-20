package com.xy.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.entity.SysArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysArticleMapper extends BaseMapper<SysArticle> {

    /**
     * 分页查询文章列表（联表分类）
     * */
    List<SysArticle> selectSysArticleList(Page<SysArticle> page, @Param("sysArticle") SysArticle sysArticle);

    /**
     * 根据文章 Id 查询文章信息（联表分类）
     * */
    SysArticle selectSysArticleById(@Param("id") Long id);
}
