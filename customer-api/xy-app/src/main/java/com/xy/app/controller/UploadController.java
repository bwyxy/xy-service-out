package com.xy.app.controller;

import com.xy.common.core.domain.R;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.system.utils.QiNiuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 客户端图片上传（头像等）
 * */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    /**
     * 单个文件大小上限 2MB
     * */
    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024;

    private final QiNiuService qiNiuService;

    /**
     * 上传图片，返回可直接访问的地址（由调用方决定这个地址用在哪）
     * */
    @PostMapping("/image")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "请选择要上传的图片");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "图片大小不能超过 2MB");
        }

        return R.ok(qiNiuService.upload(file));
    }
}
