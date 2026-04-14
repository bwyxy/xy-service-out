package com.todo.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.todo.pojo.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据层
 * */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
