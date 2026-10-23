package com.xy.app.service;

import com.xy.app.pojo.dto.TodoUserSettingUpdateDTO;
import com.xy.app.pojo.vo.TodoUserSettingVO;
import com.xy.common.entity.todo.TodoUserSetting;

/**
 * 用户偏好设置（当前只覆盖提醒相关的开关与免打扰时段）
 * */
public interface IUserSettingService {

    /**
     * 查当前用户的提醒设置，没有记录时返回默认值
     * */
    TodoUserSettingVO getSetting();

    /**
     * 修改当前用户的提醒设置（只传了值的字段才更新）
     * */
    void updateSetting(TodoUserSettingUpdateDTO dto);

    /**
     * 取指定用户的设置，没有记录时返回一份默认值（调度器用，不走登录态）
     * */
    TodoUserSetting getOrDefault(Long uId);
}
