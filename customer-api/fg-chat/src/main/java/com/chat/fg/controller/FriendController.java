package com.chat.fg.controller;

import com.chat.fg.pojo.dto.FriendRequestDTO;
import com.chat.fg.pojo.dto.HandleFriendRequestDTO;
import com.chat.fg.pojo.vo.FriendRequestVO;
import com.chat.fg.pojo.vo.FriendVO;
import com.chat.fg.service.IFriendService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.core.domain.R;
import com.xy.common.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 好友 控制层
 * */
@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final IFriendService friendService;

    /**
     * 搜索用户
     * */
    @GetMapping("/search")
    public R<List<Map<String, Object>>> searchUsers(@RequestParam String keyword) {
        Long currentUid = SecurityUtils.getUId();
        List<User> users = friendService.searchUsers(currentUid, keyword);
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("uId", u.getUId());
            map.put("nickname", u.getNickname());
            map.put("avatar", u.getAvatar());
            map.put("phone", u.getPhone());
            map.put("email", u.getEmail());
            map.put("sex", u.getSex());
            return map;
        }).toList();
        return R.ok(result);
    }

    /**
     * 发送好友请求
     * */
    @PostMapping("/request/send")
    public R<?> sendFriendRequest(@Valid @RequestBody FriendRequestDTO dto) {
        Long currentUid = SecurityUtils.getUId();
        friendService.sendFriendRequest(currentUid, dto);
        return R.ok("发送成功");
    }

    /**
     * 处理好友请求
     * */
    @PostMapping("/request/handle")
    public R<?> handleFriendRequest(@Valid @RequestBody HandleFriendRequestDTO dto) {
        Long currentUid = SecurityUtils.getUId();
        friendService.handleFriendRequest(currentUid, dto);
        return R.ok("处理成功");
    }

    /**
     * 我收到的好友请求
     * */
    @GetMapping("/request/received")
    public R<List<FriendRequestVO>> listReceivedRequests(@RequestParam(required = false) Integer status) {
        Long currentUid = SecurityUtils.getUId();
        return R.ok(friendService.listReceivedRequests(currentUid, status));
    }

    /**
     * 我发送的好友请求
     * */
    @GetMapping("/request/sent")
    public R<List<FriendRequestVO>> listSentRequests(@RequestParam(required = false) Integer status) {
        Long currentUid = SecurityUtils.getUId();
        return R.ok(friendService.listSentRequests(currentUid, status));
    }

    /**
     * 我的好友列表
     * */
    @GetMapping("/list")
    public R<List<FriendVO>> listFriends() {
        Long currentUid = SecurityUtils.getUId();
        return R.ok(friendService.listFriends(currentUid));
    }
}
