package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.chat.UserBlacklist;
import com.xy.common.enums.BusinessType;
import com.xy.common.utils.SecurityUtils;
import com.xy.common.vo.chat.ChatSessionVO;
import com.xy.common.vo.chat.ChatUserQuery;
import com.xy.common.vo.chat.ChatUserVO;
import com.xy.common.vo.chat.UserBlacklistVO;
import com.xy.system.service.IChatSessionService;
import com.xy.system.service.IChatUserService;
import com.xy.system.service.IUserBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户端用户管理（t_user）
 *
 * <p>提供用户列表、在线状态、登录设备、强制下线、黑名单、禁用启用等运营能力。</p>
 */
@RestController
@RequestMapping("/system/chat-user")
@RequiredArgsConstructor
public class ChatUserController {

    private final IChatUserService chatUserService;
    private final IChatSessionService chatSessionService;
    private final IUserBlacklistService userBlacklistService;

    /**
     * 查询客户端用户列表
     */
    @PreAuthorize("@auth.hasPermi('system:chatuser:list')")
    @PostMapping("/list")
    public R<PageResult<ChatUserVO>> list(@RequestBody ChatUserQuery query) {
        return R.ok(chatUserService.selectChatUserPage(query));
    }

    /**
     * 搜索用户（用于黑名单选择目标用户）
     */
    @PreAuthorize("@auth.hasPermi('system:chatuser:list')")
    @GetMapping("/search")
    public R<List<ChatUserVO>> search(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer limit) {
        return R.ok(chatUserService.searchChatUsers(keyword, limit));
    }

    /**
     * 查询用户详情
     */
    @PreAuthorize("@auth.hasPermi('system:chatuser:query')")
    @GetMapping("/{uId}")
    public R<ChatUserVO> detail(@PathVariable Long uId) {
        return R.ok(chatUserService.getChatUserDetail(uId));
    }

    /**
     * 修改用户状态（禁用后会自动清退全部在线设备）
     */
    @PreAuthorize("@auth.hasPermi('system:chatuser:edit')")
    @Log(title = "客户端用户", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public R<Void> updateStatus(@RequestBody ChatUserQuery query) {
        chatUserService.updateStatus(query.getUId(), query.getStatus());
        return R.ok();
    }

    /**
     * 查询用户登录设备
     */
    @PreAuthorize("@auth.hasPermi('system:chatuser:query')")
    @GetMapping("/{uId}/sessions")
    public R<List<ChatSessionVO>> sessions(@PathVariable Long uId) {
        return R.ok(chatSessionService.listActiveSessions(uId));
    }

    /**
     * 强制下线指定设备
     */
    @PreAuthorize("@auth.hasPermi('system:chatuser:force')")
    @Log(title = "客户端用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/session/{token}")
    public R<Void> forceLogout(@PathVariable String token) {
        chatSessionService.forceLogout(token, "您的账号已被管理员强制下线");
        return R.ok();
    }

    /**
     * 强制下线用户全部设备
     */
    @PreAuthorize("@auth.hasPermi('system:chatuser:force')")
    @Log(title = "客户端用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{uId}/sessions")
    public R<Void> forceLogoutAll(@PathVariable Long uId) {
        chatSessionService.forceLogoutAll(uId, "您的账号已被管理员强制下线");
        return R.ok();
    }

    /**
     * 查询用户黑名单
     */
    @PreAuthorize("@auth.hasPermi('system:chatuser:blacklist')")
    @GetMapping("/{uId}/blacklist")
    public R<PageResult<UserBlacklistVO>> blacklist(@PathVariable Long uId,
                                                    @RequestParam(required = false) Long targetUid,
                                                    @RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(userBlacklistService.selectBlacklistPage(uId, targetUid, pageNum, pageSize));
    }

    /**
     * 新增黑名单（后台代管）
     */
    @PreAuthorize("@auth.hasPermi('system:chatuser:blacklist')")
    @Log(title = "用户黑名单", businessType = BusinessType.INSERT)
    @PostMapping("/blacklist")
    public R<Void> addBlacklist(@RequestBody UserBlacklist blacklist) {
        userBlacklistService.addBlacklist(
                blacklist.getUId(),
                blacklist.getTargetUid(),
                blacklist.getRemark(),
                SecurityUtils.getLoginUser().getUsername()
        );
        return R.ok();
    }

    /**
     * 移除黑名单
     */
    @PreAuthorize("@auth.hasPermi('system:chatuser:blacklist')")
    @Log(title = "用户黑名单", businessType = BusinessType.DELETE)
    @DeleteMapping("/blacklist/{id}")
    public R<Void> removeBlacklist(@PathVariable Long id) {
        userBlacklistService.removeBlacklist(id);
        return R.ok();
    }
}
