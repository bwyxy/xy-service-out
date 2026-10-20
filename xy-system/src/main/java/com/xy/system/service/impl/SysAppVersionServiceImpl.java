package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.AppVersion;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import com.xy.common.vo.AppUpdateVO;
import com.xy.system.mapper.AppVersionMapper;
import com.xy.system.service.ISysAppVersionService;
import com.xy.system.utils.QiNiuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 应用版本服务实现
 * */
@Service
@RequiredArgsConstructor
public class SysAppVersionServiceImpl extends ServiceImpl<AppVersionMapper, AppVersion> implements ISysAppVersionService {

    private final QiNiuService qiNiuService;

    @Override
    public PageResult<AppVersion> selectAppVersionList(AppVersion query) {
        int pageNum = query.getPageNum() <= 0 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() <= 0 ? 10 : query.getPageSize();

        LambdaQueryWrapper<AppVersion> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(query.getAppKey())) {
            queryWrapper.eq(AppVersion::getAppKey, query.getAppKey());
        }
        if (StringUtils.isNotEmpty(query.getPlatform())) {
            queryWrapper.eq(AppVersion::getPlatform, query.getPlatform());
        }
        if (StringUtils.isNotEmpty(query.getVersionName())) {
            queryWrapper.like(AppVersion::getVersionName, query.getVersionName());
        }
        if (StringUtils.isNotEmpty(query.getStatus())) {
            queryWrapper.eq(AppVersion::getStatus, query.getStatus());
        }
        queryWrapper.orderByAsc(AppVersion::getAppKey)
                .orderByAsc(AppVersion::getPlatform)
                .orderByDesc(AppVersion::getVersionCode);

        Page<AppVersion> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        page.getRecords().forEach(record -> record.setFileSizeText(formatSize(record.getFileSize())));
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 字节数转可读文本
     * */
    private String formatSize(Long size) {
        if (size == null || size <= 0) {
            return "-";
        }
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        }
        return String.format("%.2f MB", size / 1024.0 / 1024.0);
    }

    @Override
    public Long insertAppVersion(AppVersion appVersion) {
        validateAppVersion(appVersion, null);
        if (!save(appVersion)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "新增版本失败");
        }
        return appVersion.getId();
    }

    @Override
    public boolean updateAppVersion(AppVersion appVersion) {
        if (appVersion.getId() == null || getById(appVersion.getId()) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "版本记录不存在");
        }
        validateAppVersion(appVersion, appVersion.getId());
        if (!updateById(appVersion)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "修改版本失败");
        }
        return true;
    }

    /**
     * 必填校验 + 同一应用与平台下版本码唯一校验。
     * 表上有 uk_app_platform_code 唯一索引兜底，这里提前拦截以给出可读提示。
     * */
    private void validateAppVersion(AppVersion appVersion, Long excludeId) {
        if (StringUtils.isEmpty(appVersion.getAppKey())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "应用标识不能为空");
        }
        if (StringUtils.isEmpty(appVersion.getPlatform())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "平台不能为空");
        }
        if (StringUtils.isEmpty(appVersion.getVersionName()) || appVersion.getVersionCode() == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "版本号与版本码不能为空");
        }
        if (StringUtils.isEmpty(appVersion.getStatus())) {
            appVersion.setStatus("0");
        }
        if (StringUtils.isEmpty(appVersion.getForceUpdate())) {
            appVersion.setForceUpdate("0");
        }

        Long exists = lambdaQuery()
                .eq(AppVersion::getAppKey, appVersion.getAppKey())
                .eq(AppVersion::getPlatform, appVersion.getPlatform())
                .eq(AppVersion::getVersionCode, appVersion.getVersionCode())
                .ne(excludeId != null, AppVersion::getId, excludeId)
                .count();
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该应用与平台下已存在相同版本码的版本");
        }
    }

    @Override
    public AppUpdateVO checkUpdate(String appKey, String platform, Integer versionCode) {
        AppUpdateVO vo = new AppUpdateVO();
        if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(platform) || versionCode == null) {
            return vo;
        }

        // 只在上架版本中取版本码最大的一条作为最新版
        AppVersion latest = lambdaQuery()
                .eq(AppVersion::getAppKey, appKey)
                .eq(AppVersion::getPlatform, platform)
                .eq(AppVersion::getStatus, "0")
                .orderByDesc(AppVersion::getVersionCode)
                .last("limit 1")
                .one();
        if (latest == null || latest.getVersionCode() <= versionCode) {
            return vo;
        }

        vo.setHasUpdate(true);
        vo.setLatestVersion(latest.getVersionName());
        vo.setLatestVersionCode(latest.getVersionCode());
        vo.setForceUpdate("1".equals(latest.getForceUpdate()));
        vo.setDownloadUrl(latest.getDownloadUrl());
        vo.setFileSize(latest.getFileSize());
        vo.setChangelog(latest.getChangelog());
        return vo;
    }

    @Override
    public String uploadPackage(Long id, MultipartFile file) {
        AppVersion version = getById(id);
        if (version == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "版本记录不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "安装包不能为空");
        }

        String url;
        try {
            url = qiNiuService.upload(file);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "安装包上传失败");
        }

        version.setDownloadUrl(url);
        version.setFileName(file.getOriginalFilename());
        version.setFileSize(file.getSize());
        updateById(version);
        return url;
    }
}
