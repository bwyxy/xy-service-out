package com.xy.admin.controller.blog;

import com.xy.common.core.domain.R;
import com.xy.common.dto.blog.DispatchPlatformSaveDTO;
import com.xy.common.entity.SysRole;
import com.xy.common.vo.blog.DispatchPlatformVO;
import com.xy.common.vo.blog.DispatchTestResultVO;
import com.xy.system.service.ISysRoleService;
import com.xy.system.service.blog.IBlogDispatchPlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分发平台配置
 * */
@RestController
@RequestMapping("/blog/dispatch/platform")
@RequiredArgsConstructor
public class BlogDispatchPlatformController {

    private final IBlogDispatchPlatformService platformService;

    private final ISysRoleService roleService;

    /**
     * 查询全部平台配置（凭证脱敏）
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:platform:list')")
    @GetMapping("/list")
    public R<List<DispatchPlatformVO>> list() {
        return R.ok(platformService.selectPlatformList());
    }

    /**
     * 查询当前登录用户可用的启用平台（分发时选择目标平台使用）
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:platform:list')")
    @GetMapping("/usable")
    public R<List<DispatchPlatformVO>> usable() {
        return R.ok(platformService.selectUsablePlatformList());
    }

    /**
     * 查询系统支持的全部平台（适配器注册中心）
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:platform:list')")
    @GetMapping("/supported")
    public R<List<DispatchPlatformVO>> supported() {
        return R.ok(platformService.selectSupportedPlatforms());
    }

    /**
     * 查询可选角色，用于配置平台使用权限
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:platform:list')")
    @GetMapping("/roles")
    public R<List<SysRole>> roles() {
        return R.ok(roleService.list());
    }

    /**
     * 查询平台配置详情
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:platform:list')")
    @GetMapping("/{id}")
    public R<DispatchPlatformVO> detail(@PathVariable Long id) {
        return R.ok(platformService.selectPlatformById(id));
    }

    /**
     * 新增平台配置
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:platform:add')")
    @PostMapping("/add")
    public R<String> add(@RequestBody DispatchPlatformSaveDTO dto) {
        platformService.addPlatform(dto);
        return R.ok("新增成功");
    }

    /**
     * 更新平台配置
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:platform:edit')")
    @PutMapping("/update")
    public R<String> update(@RequestBody DispatchPlatformSaveDTO dto) {
        platformService.updatePlatform(dto);
        return R.ok("更新成功");
    }

    /**
     * 删除平台配置
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:platform:remove')")
    @DeleteMapping("/{id}")
    public R<String> remove(@PathVariable Long id) {
        platformService.deletePlatform(id);
        return R.ok("删除成功");
    }

    /**
     * 平台连通性测试
     * */
    @PreAuthorize("@auth.hasPermi('blog:dispatch:platform:test')")
    @PostMapping("/test/{id}")
    public R<DispatchTestResultVO> test(@PathVariable Long id) {
        return R.ok(platformService.testConnection(id));
    }
}
