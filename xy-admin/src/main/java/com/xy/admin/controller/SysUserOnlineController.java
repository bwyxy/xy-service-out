package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.core.text.Convert;
import com.xy.common.entity.SysUserOnline;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.ISysUserOnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 在线用户管理 控制层
 * */
@RestController
@RequestMapping("/system/online")
@RequiredArgsConstructor
public class SysUserOnlineController {

    private final ISysUserOnlineService userOnlineService;

    /**
     * 查询在线用户列表
     * */
    @PreAuthorize("@auth.hasPermi('system:online:list')")
    @GetMapping("/list")
    public R<PageResult<SysUserOnline>> list(@RequestParam(required = false) Map<String, Object> query) {
        if (query == null) {
            query = Collections.emptyMap();
        }
        String ipaddr = Convert.toStr(query.get("ipaddr"), "");
        String username = Convert.toStr(query.get("username"), "");
        List<SysUserOnline> list = userOnlineService.listOnlineUsers(ipaddr, username);

        int pageNum = Convert.toInt(query.get("pageNum"), 1);
        int pageSize = Convert.toInt(query.get("pageSize"), 15);
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, list.size());
        List<SysUserOnline> records = from < list.size() ? list.subList(from, to) : Collections.emptyList();

        return R.ok(PageResult.of(records, (long) list.size(), (long) pageNum, (long) pageSize));
    }

    /**
     * 强退在线用户
     * */
    @PreAuthorize("@auth.hasPermi('system:online:force')")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenId}")
    public R<Void> forceLogout(@PathVariable String tokenId) {
        userOnlineService.forceLogout(tokenId);
        return R.ok();
    }
}
