package com.chat.fg.controller;

import com.chat.fg.service.IUserService;
import com.xy.common.core.domain.R;
import com.xy.system.utils.QiNiuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 上传
 * */
@RequestMapping("/upload")
@RestController
@RequiredArgsConstructor
public class UploadController {

    @Autowired
    private QiNiuService qiNiuService;

    @Autowired
    private IUserService userService;

    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024;

    /**
     * 更新用户头像
     * */
    @PostMapping("/avatar")
    public R<?> updateAvatar(@RequestParam("file") MultipartFile file) throws IOException {

        long fileSize = file.getSize();

        if (fileSize > MAX_SIZE_BYTES) {
            return R.error(404, "图片大小不能超过2MB");
        }

        String url = qiNiuService.upload(file);

        userService.updateUserAvatar(url);

        return R.ok("修改头像成功");
    }

    /**
     * 上传图片
     * */
    @PostMapping("/image")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {

        long fileSize = file.getSize();

        if (fileSize > MAX_SIZE_BYTES) {
            return R.error(404, "图片大小不能超过2MB");
        }

        String url = qiNiuService.upload(file);

        return R.ok(url);
    }
}
