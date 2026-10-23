package com.xy.app.controller;

import com.xy.app.pojo.dto.TodoUserSettingUpdateDTO;
import com.xy.app.pojo.vo.TodoUserSettingVO;
import com.xy.app.service.IUserSettingService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户提醒偏好设置
 * */
@RestController
@RequestMapping("/user/setting")
@RequiredArgsConstructor
public class UserSettingController {

    private final IUserSettingService userSettingService;

    /**
     * 查当前用户的提醒设置
     * */
    @GetMapping
    public R<TodoUserSettingVO> get() {
        return R.ok(userSettingService.getSetting());
    }

    /**
     * 修改提醒设置（只更新传了值的字段）
     * */
    @PutMapping
    public R<Void> update(@RequestBody TodoUserSettingUpdateDTO dto) {
        userSettingService.updateSetting(dto);

        return R.ok();
    }
}
