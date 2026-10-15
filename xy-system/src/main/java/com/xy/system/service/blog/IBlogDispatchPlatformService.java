package com.xy.system.service.blog;

import com.xy.common.dto.blog.DispatchPlatformSaveDTO;
import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.vo.blog.DispatchPlatformVO;
import com.xy.common.vo.blog.DispatchTestResultVO;

import java.util.List;
import java.util.Map;

/**
 * 分发平台配置服务
 * */
public interface IBlogDispatchPlatformService {

    /**
     * 查询全部平台配置（凭证脱敏）
     * */
    List<DispatchPlatformVO> selectPlatformList();

    /**
     * 查询当前登录用户可用的启用平台（用于分发时选择目标平台）
     * */
    List<DispatchPlatformVO> selectUsablePlatformList();

    /**
     * 查询系统支持的全部平台（来源于适配器注册中心，用于新增平台配置时选择）
     * */
    List<DispatchPlatformVO> selectSupportedPlatforms();

    /**
     * 查询平台配置详情
     * */
    DispatchPlatformVO selectPlatformById(Long id);

    /**
     * 按平台编码查询平台配置
     * */
    BlogDispatchPlatform getByCode(String platformCode);

    /**
     * 新增平台配置
     * */
    void addPlatform(DispatchPlatformSaveDTO dto);

    /**
     * 更新平台配置
     * */
    void updatePlatform(DispatchPlatformSaveDTO dto);

    /**
     * 删除平台配置
     * */
    void deletePlatform(Long id);

    /**
     * 平台连通性测试
     * */
    DispatchTestResultVO testConnection(Long id);

    /**
     * 校验当前登录用户是否有使用该平台的权限
     * */
    boolean canUsePlatform(String platformCode);

    /**
     * 解密平台凭证，失败或未配置时返回空 Map
     * */
    Map<String, String> decryptCredentials(BlogDispatchPlatform platform);
}
