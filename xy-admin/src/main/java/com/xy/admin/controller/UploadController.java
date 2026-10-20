package com.xy.admin.controller;

import com.xy.common.core.domain.R;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.system.utils.QiNiuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * 文件资源
 * */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    /** 通用文件（安装包）允许的后缀 */
    private static final Set<String> FILE_SUFFIXES = Set.of(
            "exe", "msi", "msix", "dmg", "zip", "apk", "deb", "rpm", "appimage");

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

    /**
     * 上传通用文件（应用安装包等）
     * */
    @PostMapping("/file")
    public R<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        int dotIndex = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
        String suffix = dotIndex < 0 ? "" : originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!FILE_SUFFIXES.contains(suffix)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "不支持的文件类型：" + suffix);
        }

        return R.ok(qiNiuService.upload(file));
    }
}
