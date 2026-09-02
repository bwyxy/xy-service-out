package com.xy.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.chat.Group;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群组表 数据层
 * */
@Mapper
public interface GroupMapper extends BaseMapper<Group> {
}
