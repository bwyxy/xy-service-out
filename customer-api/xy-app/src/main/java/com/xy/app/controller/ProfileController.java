package com.xy.app.controller;

import com.xy.app.pojo.dto.TodoProfileUpdateDTO;
import com.xy.app.service.IProfileService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人资料
 * */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final IProfileService profileService;

    /**
     * 修改个人资料（昵称 / 头像 / 性别，只更新传了值的字段）
     * */
    @PutMapping
    public R<Void> update(@RequestBody TodoProfileUpdateDTO dto) {
        profileService.updateProfile(dto);

        return R.ok();
    }
}
