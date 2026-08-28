package com.xy.admin.controller;

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
 * 文件资源
 * */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    @Autowired
    private QiNiuService qiNiuService;

    /**
     * 上传图片
     * */
    @PostMapping("/image")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        String url = qiNiuService.upload(file);

        return R.ok(url);
    }
}
