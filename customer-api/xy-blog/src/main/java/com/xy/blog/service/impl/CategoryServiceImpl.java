package com.xy.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.blog.service.ICategoryService;
import com.xy.common.entity.SysCategory;
import com.xy.common.utils.StringUtils;
import com.xy.common.vo.blog.CategoryVO;
import com.xy.common.vo.blog.ToolCategoryVO;
import com.xy.system.mapper.SysCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<SysCategoryMapper, SysCategory> implements ICategoryService {

    /**
     * 获取全部分类列表
     * */
    @Override
    public List<SysCategory> getCategoryAll(String keyword) {

        LambdaQueryWrapper<SysCategory> queryWrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !StringUtils.isEmpty(keyword)) {
            queryWrapper.like(SysCategory::getName, keyword);
        }

        return this.list(queryWrapper);
    }

    /**
     * 查询分类关联文章列表
     * */
    @Override
    public List<CategoryVO> listCategoryWithArticles() {
        return this.baseMapper.listCategoryWithArticles();
    }

    /**
     * 查询分类关联工具列表
     * */
    public List<ToolCategoryVO> listCategoryWithTools() {
        return this.baseMapper.listCategoryWithTools();
    }

    /**
     * 查询相册分类
     * */
    @Override
    public List<SysCategory> listAlbumCategory() {
        LambdaQueryWrapper<SysCategory> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysCategory::getType, "album");

        return this.list(queryWrapper);
    }

    /**
     * 查询工具分类
     * */
    @Override
    public List<SysCategory> listToolCategory() {
        LambdaQueryWrapper<SysCategory> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysCategory::getType, "tool");

        return this.list(queryWrapper);
    }
}
