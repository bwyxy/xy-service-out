package com.xy.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.AppVersion;
import com.xy.common.vo.AppUpdateVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 应用版本服务
 * */
public interface ISysAppVersionService extends IService<AppVersion> {

    /**
     * 管理端分页查询
     * */
    PageResult<AppVersion> selectAppVersionList(AppVersion query);

    /**
     * 新增版本（校验同一应用+平台下版本码不重复）
     * */
    Long insertAppVersion(AppVersion appVersion);

    /**
     * 修改版本（校验同一应用+平台下版本码不重复）
     * */
    boolean updateAppVersion(AppVersion appVersion);

    /**
     * 客户端检查更新：返回该应用+平台下的最新版本信息
     *
     * @param appKey      应用标识
     * @param platform    平台 web/windows/macos
     * @param versionCode 客户端当前版本码
     * */
    AppUpdateVO checkUpdate(String appKey, String platform, Integer versionCode);

    /**
     * 为已存在的版本记录上传/替换安装包，并回填地址、文件名与大小
     * */
    String uploadPackage(Long id, MultipartFile file);
}
