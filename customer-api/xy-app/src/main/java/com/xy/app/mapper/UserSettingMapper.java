package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoUserSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好设置
 * */
@Mapper
public interface UserSettingMapper extends BaseMapper<TodoUserSetting> {
}
