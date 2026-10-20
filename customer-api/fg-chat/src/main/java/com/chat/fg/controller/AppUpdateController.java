package com.chat.fg.controller;

import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import com.xy.common.vo.AppUpdateVO;
import com.xy.system.service.ISysAppVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端应用更新
 * */
@RestController
@RequestMapping("/app/version")
@RequiredArgsConstructor
public class AppUpdateController {

    private final ISysAppVersionService appVersionService;

    /**
     * 检查更新。
     * 免登录（@Anonymous）：未登录时也能收到强制更新提示。
     *
     * @param appKey      应用标识
     * @param platform    平台 web/windows/macos
     * @param versionCode 客户端当前版本码
     * */
    @Anonymous
    @GetMapping("/check")
    public R<AppUpdateVO> check(@RequestParam String appKey,
                                @RequestParam String platform,
                                @RequestParam Integer versionCode) {
        return R.ok(appVersionService.checkUpdate(appKey, platform, versionCode));
    }
}
