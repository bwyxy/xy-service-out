package com.xy.system.service.impl.blog;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.blog.BlogMenuConfig;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.blog.BlogMenuConfigMapper;
import com.xy.system.service.blog.IBlogMenuConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BlogMenuConfigServiceImpl extends ServiceImpl<BlogMenuConfigMapper, BlogMenuConfig> implements IBlogMenuConfigService {

    /**
     * 根据菜单Id获取菜单配置列表
     * */
    @Override
    public List<BlogMenuConfig> selectBlogMenuConfigByMenuId(Long menuId) {
        if (menuId == null) {
            throw new ServiceException("菜单Id不能为空");
        }
        LambdaQueryWrapper<BlogMenuConfig> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(BlogMenuConfig::getMenuId, menuId);

        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 获取菜单配置信息
     * */
    @Override
    public BlogMenuConfig selectBlogMenuConfigById(Long id) {
        if (id == null) {
            throw new ServiceException("菜单配置Id不能为空");
        }
        LambdaQueryWrapper<BlogMenuConfig> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(BlogMenuConfig::getId, id);

        return this.getOne(queryWrapper);
    }

    /**
     *  根据菜单 Id获取菜单配置信息
     * */
    @Override
    public Map<String, Object> getBlogMenuConfigByMenuId(Long menuId) {
        Map<String, Object> configMap;

        List<BlogMenuConfig> list = selectBlogMenuConfigByMenuId(menuId);

        configMap = list.stream().collect(Collectors.toMap(
                BlogMenuConfig::getConfigKey,
                item -> parseConfigValue(item.getConfigValue())
        ));

        return Collections.unmodifiableMap(configMap);
    }

    private Object parseConfigValue(String value) {
        if (value == null) return null;
        // 尝试解析为 JSON 对象或数组
        try {
            return JSON.parse(value);
        } catch (Exception e) {
            // 不是合法 JSON，直接返回字符串
            return value;
        }
    }

    /**
     * 添加菜单配置
     * */
    @Override
    public void addBlogMenuConfig(BlogMenuConfig blogMenuConfig) {
        if (blogMenuConfig.getMenuId() == null) {
            throw new ServiceException("菜单Id不能为空");
        }
        if (blogMenuConfig.getConfigKey() == null || StringUtils.isEmpty(blogMenuConfig.getConfigKey())) {
            throw new ServiceException("配置标识不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        blogMenuConfig.setCreateTime(now);
        blogMenuConfig.setUpdateTime(now);

        save(blogMenuConfig);
    }

    /**
     * 编辑菜单配置
     * */
    @Override
    public void editBlogMenuConfig(BlogMenuConfig blogMenuConfig) {
        if (blogMenuConfig.getId() == null) {
            throw new ServiceException("配置Id不能为空");
        }
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<BlogMenuConfig> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(BlogMenuConfig::getId, blogMenuConfig.getId())
                .set(BlogMenuConfig::getConfigType, blogMenuConfig.getConfigType())
                .set(BlogMenuConfig::getConfigValue, blogMenuConfig.getConfigValue())
                .set(BlogMenuConfig::getUpdateTime, now);

        update(updateWrapper);
    }

    /**
     * 根据配置Id删除菜单配置
     * */
    @Override
    public void deleteBlogMenuConfig(Long id) {
        if (id==null){
           throw new ServiceException("配置Id不能为空");
        }

        LambdaQueryWrapper<BlogMenuConfig> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(BlogMenuConfig::getId, id);

        remove(queryWrapper);
    }
}
