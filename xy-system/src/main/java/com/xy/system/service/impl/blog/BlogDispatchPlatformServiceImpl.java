package com.xy.system.service.impl.blog;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.dto.blog.DispatchPlatformSaveDTO;
import com.xy.common.entity.SysRole;
import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.entity.blog.BlogDispatchPlatformRole;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.SecurityUtils;
import com.xy.common.utils.StringUtils;
import com.xy.common.vo.blog.DispatchPlatformVO;
import com.xy.common.vo.blog.DispatchTestResultVO;
import com.xy.system.dispatch.adapter.DispatchAdapterRegistry;
import com.xy.system.dispatch.adapter.DispatchPlatformAdapter;
import com.xy.system.dispatch.support.DispatchCredentialCrypto;
import com.xy.system.mapper.blog.BlogDispatchPlatformMapper;
import com.xy.system.mapper.blog.BlogDispatchPlatformRoleMapper;
import com.xy.system.service.ISysRoleService;
import com.xy.system.service.blog.IBlogDispatchPlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分发平台配置服务实现
 * 平台凭证统一以 AES 密文存储，任何查询接口都不返回凭证内容
 * */
@Slf4j
@Service
public class BlogDispatchPlatformServiceImpl extends ServiceImpl<BlogDispatchPlatformMapper, BlogDispatchPlatform>
        implements IBlogDispatchPlatformService {

    private static final int DEFAULT_TITLE_MAX_LENGTH = 100;

    private static final int DEFAULT_TAG_MAX_COUNT = 5;

    private static final int DEFAULT_SUMMARY_MAX_LENGTH = 200;

    private static final int DEFAULT_CONTENT_MAX_LENGTH = 100000;

    private static final int DEFAULT_RATE_LIMIT = 30;

    private static final int DEFAULT_TIMEOUT_SECONDS = 15;

    private static final int DEFAULT_MAX_RETRY = 3;

    private final BlogDispatchPlatformRoleMapper platformRoleMapper;

    private final DispatchAdapterRegistry adapterRegistry;

    private final DispatchCredentialCrypto credentialCrypto;

    private final ISysRoleService roleService;

    public BlogDispatchPlatformServiceImpl(BlogDispatchPlatformRoleMapper platformRoleMapper,
                                           DispatchAdapterRegistry adapterRegistry,
                                           DispatchCredentialCrypto credentialCrypto,
                                           ISysRoleService roleService) {
        this.platformRoleMapper = platformRoleMapper;
        this.adapterRegistry = adapterRegistry;
        this.credentialCrypto = credentialCrypto;
        this.roleService = roleService;
    }

    @Override
    public List<DispatchPlatformVO> selectPlatformList() {
        LambdaQueryWrapper<BlogDispatchPlatform> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(BlogDispatchPlatform::getSort).orderByAsc(BlogDispatchPlatform::getId);
        List<BlogDispatchPlatform> platforms = this.list(wrapper);
        Map<String, List<Long>> roleBindings = loadAllRoleBindings();
        List<DispatchPlatformVO> result = new ArrayList<>(platforms.size());
        for (BlogDispatchPlatform platform : platforms) {
            result.add(toVO(platform, roleBindings.getOrDefault(platform.getPlatformCode(), Collections.emptyList())));
        }
        return result;
    }

    @Override
    public List<DispatchPlatformVO> selectUsablePlatformList() {
        List<DispatchPlatformVO> platforms = selectPlatformList();
        List<DispatchPlatformVO> result = new ArrayList<>();
        for (DispatchPlatformVO platform : platforms) {
            if (platform.getEnabled() != null && platform.getEnabled() == 1
                    && canUsePlatform(platform.getPlatformCode())) {
                result.add(platform);
            }
        }
        return result;
    }

    @Override
    public List<DispatchPlatformVO> selectSupportedPlatforms() {
        List<DispatchPlatformVO> result = new ArrayList<>();
        for (DispatchPlatformAdapter adapter : adapterRegistry.all()) {
            DispatchPlatformVO vo = new DispatchPlatformVO();
            vo.setPlatformCode(adapter.platformCode());
            vo.setPlatformName(adapter.platformName());
            vo.setAdapterSupported(true);
            vo.setRequiredCredentialKeys(adapter.requiredCredentialKeys());
            vo.setEnabled(0);
            result.add(vo);
        }
        return result;
    }

    @Override
    public DispatchPlatformVO selectPlatformById(Long id) {
        if (id == null) {
            throw new ServiceException("平台Id 不能为空");
        }
        BlogDispatchPlatform platform = getById(id);
        if (platform == null) {
            throw new ServiceException("平台配置不存在");
        }
        return toVO(platform, loadRoleIds(platform.getPlatformCode()));
    }

    @Override
    public BlogDispatchPlatform getByCode(String platformCode) {
        if (StringUtils.isEmpty(platformCode)) {
            return null;
        }
        LambdaQueryWrapper<BlogDispatchPlatform> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDispatchPlatform::getPlatformCode, platformCode.trim());
        return getOne(wrapper, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPlatform(DispatchPlatformSaveDTO dto) {
        String platformCode = validateAndGetCode(dto);
        if (platformCodeExists(platformCode, null)) {
            throw new ServiceException("平台编码已存在：" + platformCode);
        }
        BlogDispatchPlatform platform = new BlogDispatchPlatform();
        applyCommonFields(platform, dto, platformCode);
        String cipher = encryptCredentials(dto.getCredentials(), Collections.emptyMap());
        platform.setCredentialCipher(cipher);
        platform.setCreateBy(currentUsername());
        platform.setUpdateBy(currentUsername());
        save(platform);
        saveRoleBindings(platformCode, dto.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlatform(DispatchPlatformSaveDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("平台Id 不能为空");
        }
        BlogDispatchPlatform platform = getById(dto.getId());
        if (platform == null) {
            throw new ServiceException("平台配置不存在");
        }
        String platformCode = validateAndGetCode(dto);
        if (!platformCode.equals(platform.getPlatformCode())) {
            if (platformCodeExists(platformCode, dto.getId())) {
                throw new ServiceException("平台编码已存在：" + platformCode);
            }
            platform.setPlatformCode(platformCode);
        }
        applyCommonFields(platform, dto, platformCode);
        if (dto.getCredentials() != null && !dto.getCredentials().isEmpty()) {
            platform.setCredentialCipher(encryptCredentials(dto.getCredentials(), decryptCredentials(platform)));
        }
        platform.setUpdateBy(currentUsername());
        updateById(platform);
        if (dto.getRoleIds() != null) {
            saveRoleBindings(platformCode, dto.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlatform(Long id) {
        if (id == null) {
            throw new ServiceException("平台Id 不能为空");
        }
        BlogDispatchPlatform platform = getById(id);
        if (platform == null) {
            throw new ServiceException("平台配置不存在");
        }
        removeById(id);
        LambdaQueryWrapper<BlogDispatchPlatformRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDispatchPlatformRole::getPlatformCode, platform.getPlatformCode());
        platformRoleMapper.delete(wrapper);
    }

    @Override
    public DispatchTestResultVO testConnection(Long id) {
        if (id == null) {
            throw new ServiceException("平台Id 不能为空");
        }
        BlogDispatchPlatform platform = getById(id);
        if (platform == null) {
            throw new ServiceException("平台配置不存在");
        }
        DispatchPlatformAdapter adapter = adapterRegistry.find(platform.getPlatformCode());
        if (adapter == null) {
            throw new ServiceException("未找到平台适配器实现：" + platform.getPlatformCode());
        }
        DispatchTestResultVO vo = new DispatchTestResultVO();
        long start = System.currentTimeMillis();
        boolean success;
        String message;
        try {
            adapter.testConnection(decryptCredentials(platform), parseConfig(platform.getConfigJson()),
                    platform, defaultInt(platform.getRequestTimeoutSeconds(), DEFAULT_TIMEOUT_SECONDS));
            success = true;
            message = "连通性测试通过";
        } catch (Exception e) {
            success = false;
            message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
        long cost = System.currentTimeMillis() - start;
        vo.setSuccess(success);
        vo.setMessage(message);
        vo.setCostMs(cost);

        LambdaUpdateWrapper<BlogDispatchPlatform> update = new LambdaUpdateWrapper<>();
        update.eq(BlogDispatchPlatform::getId, id)
                .set(BlogDispatchPlatform::getLastTestTime, LocalDateTime.now())
                .set(BlogDispatchPlatform::getLastTestResult, success ? "成功：" + message : "失败：" + message);
        update(update);
        return vo;
    }

    @Override
    public boolean canUsePlatform(String platformCode) {
        List<Long> boundRoleIds = loadRoleIds(platformCode);
        if (boundRoleIds.isEmpty()) {
            return true;
        }
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        Long userId = SecurityUtils.getUserId();
        Set<Long> ownRoleIds = roleService.selectRolesByUserId(userId).stream()
                .map(SysRole::getId).collect(Collectors.toSet());
        return boundRoleIds.stream().anyMatch(ownRoleIds::contains);
    }

    @Override
    public Map<String, String> decryptCredentials(BlogDispatchPlatform platform) {
        Map<String, String> credentials = new LinkedHashMap<>();
        if (platform == null) {
            return credentials;
        }
        String plain = credentialCrypto.decrypt(platform.getCredentialCipher());
        if (StringUtils.isEmpty(plain)) {
            return credentials;
        }
        try {
            JSONObject json = JSONUtil.parseObj(plain);
            for (String key : json.keySet()) {
                Object value = json.get(key);
                if (value != null) {
                    credentials.put(key, String.valueOf(value));
                }
            }
        } catch (Exception e) {
            log.warn("平台凭证解析失败 platform={} err={}", platform.getPlatformCode(), e.getMessage());
        }
        return credentials;
    }

    private String validateAndGetCode(DispatchPlatformSaveDTO dto) {
        if (dto == null || StringUtils.isEmpty(dto.getPlatformCode())) {
            throw new ServiceException("平台编码不能为空");
        }
        String platformCode = dto.getPlatformCode().trim();
        if (adapterRegistry.find(platformCode) == null) {
            throw new ServiceException("暂不支持该平台，未找到对应的适配器实现：" + platformCode);
        }
        return platformCode;
    }

    private void applyCommonFields(BlogDispatchPlatform platform, DispatchPlatformSaveDTO dto, String platformCode) {
        platform.setPlatformCode(platformCode);
        platform.setPlatformName(StringUtils.isEmpty(dto.getPlatformName()) ? platformCode : dto.getPlatformName());
        platform.setIcon(dto.getIcon());
        platform.setDescription(dto.getDescription());
        platform.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        if (dto.getConfig() != null) {
            platform.setConfigJson(JSONUtil.toJsonStr(dto.getConfig()));
        } else if (platform.getConfigJson() == null) {
            platform.setConfigJson("{}");
        }
        platform.setTitleMaxLength(defaultInt(dto.getTitleMaxLength(), DEFAULT_TITLE_MAX_LENGTH));
        platform.setTagMaxCount(defaultInt(dto.getTagMaxCount(), DEFAULT_TAG_MAX_COUNT));
        platform.setSummaryMaxLength(defaultInt(dto.getSummaryMaxLength(), DEFAULT_SUMMARY_MAX_LENGTH));
        platform.setContentMaxLength(defaultInt(dto.getContentMaxLength(), DEFAULT_CONTENT_MAX_LENGTH));
        platform.setRateLimitPerMinute(defaultInt(dto.getRateLimitPerMinute(), DEFAULT_RATE_LIMIT));
        platform.setRequestTimeoutSeconds(defaultInt(dto.getRequestTimeoutSeconds(), DEFAULT_TIMEOUT_SECONDS));
        platform.setMaxRetry(defaultInt(dto.getMaxRetry(), DEFAULT_MAX_RETRY));
        platform.setSort(dto.getSort() == null ? 0 : dto.getSort());
        platform.setRemark(dto.getRemark());
    }

    private String encryptCredentials(Map<String, String> incoming, Map<String, String> existing) {
        Map<String, String> merged = new LinkedHashMap<>(existing);
        if (incoming != null) {
            incoming.forEach((key, value) -> {
                if (key != null && value != null && !value.isBlank()) {
                    merged.put(key.trim(), value.trim());
                }
            });
        }
        if (merged.isEmpty()) {
            return null;
        }
        return credentialCrypto.encrypt(JSONUtil.toJsonStr(merged));
    }

    private boolean platformCodeExists(String platformCode, Long excludeId) {
        LambdaQueryWrapper<BlogDispatchPlatform> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDispatchPlatform::getPlatformCode, platformCode);
        if (excludeId != null) {
            wrapper.ne(BlogDispatchPlatform::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    private void saveRoleBindings(String platformCode, List<Long> roleIds) {
        LambdaQueryWrapper<BlogDispatchPlatformRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDispatchPlatformRole::getPlatformCode, platformCode);
        platformRoleMapper.delete(wrapper);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Set<Long> distinct = new LinkedHashSet<>(roleIds);
        for (Long roleId : distinct) {
            if (roleId == null) {
                continue;
            }
            BlogDispatchPlatformRole binding = new BlogDispatchPlatformRole();
            binding.setPlatformCode(platformCode);
            binding.setRoleId(roleId);
            platformRoleMapper.insert(binding);
        }
    }

    private List<Long> loadRoleIds(String platformCode) {
        if (StringUtils.isEmpty(platformCode)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<BlogDispatchPlatformRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDispatchPlatformRole::getPlatformCode, platformCode);
        return platformRoleMapper.selectList(wrapper).stream()
                .map(BlogDispatchPlatformRole::getRoleId).collect(Collectors.toList());
    }

    private Map<String, List<Long>> loadAllRoleBindings() {
        return platformRoleMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(BlogDispatchPlatformRole::getPlatformCode,
                        Collectors.mapping(BlogDispatchPlatformRole::getRoleId, Collectors.toList())));
    }

    private DispatchPlatformVO toVO(BlogDispatchPlatform platform, List<Long> roleIds) {
        DispatchPlatformVO vo = new DispatchPlatformVO();
        vo.setId(platform.getId());
        vo.setPlatformCode(platform.getPlatformCode());
        vo.setPlatformName(platform.getPlatformName());
        vo.setIcon(platform.getIcon());
        vo.setDescription(platform.getDescription());
        vo.setEnabled(platform.getEnabled());
        Map<String, String> credentials = decryptCredentials(platform);
        vo.setCredentialConfigured(!credentials.isEmpty());
        vo.setCredentialKeys(new ArrayList<>(credentials.keySet()));
        DispatchPlatformAdapter adapter = adapterRegistry.find(platform.getPlatformCode());
        vo.setAdapterSupported(adapter != null);
        vo.setRequiredCredentialKeys(adapter == null ? Collections.emptyList() : adapter.requiredCredentialKeys());
        vo.setConfig(parseConfig(platform.getConfigJson()));
        vo.setTitleMaxLength(platform.getTitleMaxLength());
        vo.setTagMaxCount(platform.getTagMaxCount());
        vo.setSummaryMaxLength(platform.getSummaryMaxLength());
        vo.setContentMaxLength(platform.getContentMaxLength());
        vo.setRateLimitPerMinute(platform.getRateLimitPerMinute());
        vo.setRequestTimeoutSeconds(platform.getRequestTimeoutSeconds());
        vo.setMaxRetry(platform.getMaxRetry());
        vo.setSort(platform.getSort());
        vo.setRoleIds(new ArrayList<>(roleIds));
        vo.setLastTestTime(platform.getLastTestTime());
        vo.setLastTestResult(platform.getLastTestResult());
        vo.setRemark(platform.getRemark());
        return vo;
    }

    private Map<String, Object> parseConfig(String configJson) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (StringUtils.isEmpty(configJson)) {
            return config;
        }
        try {
            JSONObject json = JSONUtil.parseObj(configJson);
            for (String key : json.keySet()) {
                config.put(key, json.get(key));
            }
        } catch (Exception e) {
            log.warn("平台发布规则解析失败 err={}", e.getMessage());
        }
        return config;
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private String currentUsername() {
        try {
            return SecurityUtils.getLoginUser().getUsername();
        } catch (Exception e) {
            return "system";
        }
    }
}
