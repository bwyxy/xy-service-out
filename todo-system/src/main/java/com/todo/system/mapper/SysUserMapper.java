package com.todo.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.todo.common.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 添加用户
     * */

}
