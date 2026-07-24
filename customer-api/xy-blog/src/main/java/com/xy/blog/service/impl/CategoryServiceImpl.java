package com.xy.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.blog.service.ICategoryService;
import com.xy.common.entity.SysCategory;
import com.xy.common.utils.StringUtils;
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
}
