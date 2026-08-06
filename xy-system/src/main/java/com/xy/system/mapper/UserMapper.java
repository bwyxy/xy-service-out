package com.xy.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 数据层
 * */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
