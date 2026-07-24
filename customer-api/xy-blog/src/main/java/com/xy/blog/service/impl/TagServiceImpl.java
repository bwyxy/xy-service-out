package com.xy.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.blog.service.ITagService;
import com.xy.common.entity.SysTag;
import com.xy.system.mapper.SysTagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<SysTagMapper, SysTag>  implements ITagService {

    @Autowired
    private SysTagMapper sysTagMapper;
    /**
     * 获取标签列表
     * */
    public List<SysTag> getSysTagList() {
        return sysTagMapper.getSysTagList();
    }

    /**
     * 获取标签列表(根据技能级别)
     * */
    public List<SysTag> getSysTagByLevel() {
        LambdaQueryWrapper<SysTag> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.isNotNull(SysTag::getLevel).orderByDesc(SysTag::getLevel);

        return list(queryWrapper);
    }
}
