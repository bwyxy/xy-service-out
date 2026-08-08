package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.mapper.FriendMapper;
import com.chat.fg.mapper.FriendRequestMapper;
import com.chat.fg.pojo.dto.FriendRequestDTO;
import com.chat.fg.pojo.dto.HandleFriendRequestDTO;
import com.chat.fg.pojo.vo.FriendRequestVO;
import com.chat.fg.pojo.vo.FriendVO;
import com.chat.fg.service.IFriendService;
import com.xy.system.service.INotificationService;
import com.chat.fg.utils.DistributedLockHelper;
import com.xy.common.entity.Friend;
import com.xy.common.entity.FriendRequest;
import com.xy.common.entity.User;
import com.xy.common.enums.FriendRequestStatus;
import com.xy.common.enums.NotificationType;
import com.xy.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 好友 逻辑层实现
 * */
@Service
@RequiredArgsConstructor
public class FriendServiceImpl extends ServiceImpl<FriendRequestMapper, FriendRequest>
        implements IFriendService {

    private final FriendRequestMapper friendRequestMapper;
    private final FriendMapper friendMapper;
    private final UserServiceImpl userService;
    private final INotificationService notificationService;
    private final DistributedLockHelper lockHelper;

    private static final String FRIEND_STATUS_NORMAL = "0";
    private static final int ACTION_ACCEPT = 1;
    private static final int ACTION_REJECT = 2;

    @Override
    public List<User> searchUsers(Long currentUid, String keyword) {
        if (currentUid == null) {
            throw new BusinessException("当前用户未登录");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String kw = keyword.trim();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(User::getNickname, kw)
                        .or()
                        .like(User::getPhone, kw)
                        .or()
                        .like(User::getEmail, kw))
                .ne(User::getUId, currentUid)
                .eq(User::getStatus, "0")
                .last("limit 20");
        return userService.list(wrapper);
    }

    @Override
    public FriendRequest sendFriendRequest(Long fromUid, FriendRequestDTO dto) {
        Long toUid = dto.getToUid();
        if (fromUid.equals(toUid)) {
            throw new BusinessException("不能添加自己为好友");
        }
        User targetUser = userService.getById(toUid);
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }
        if (!"0".equals(targetUser.getStatus())) {
            throw new BusinessException("目标用户已被禁用");
        }

        // 校验是否已经是好友
        if (isFriend(fromUid, toUid)) {
            throw new BusinessException("对方已经是您的好友");
        }

        String lockKey = buildPairLockKey("friend:request", fromUid, toUid);
        // 建立 redis分布式锁防止并非发操作
        return lockHelper.tryLock(lockKey, 3, 10, () -> {
            // 双重检查是否已经是好友
            if (isFriend(fromUid, toUid)) {
                throw new BusinessException("对方已经是您的好友");
            }
            // 检查是否存在待处理请求
            FriendRequest exist = getPendingRequest(fromUid, toUid);
            if (exist != null) {
                throw new BusinessException("您已向该用户发送过好友请求，请勿重复发送");
            }
            // 检查对方是否已向我发送请求，如果是，直接同意
            FriendRequest reverse = getPendingRequest(toUid, fromUid);
            if (reverse != null) {
                acceptRequest(reverse, fromUid, "双方同时发起请求，已自动同意");
                return reverse;
            }

            FriendRequest request = new FriendRequest();
            request.setFromUid(fromUid);
            request.setToUid(toUid);
            request.setStatus(FriendRequestStatus.PENDING.getCode());
            request.setApplyMsg(dto.getApplyMsg());
            LocalDateTime now = LocalDateTime.now();
            request.setCreateTime(now);
            request.setUpdateTime(now);
            friendRequestMapper.insert(request);

            User fromUser = userService.getById(fromUid);
            String nickname = fromUser == null ? "用户" + fromUid : fromUser.getNickname();
            notificationService.notifyUser(toUid, NotificationType.FRIEND_REQUEST,
                    "好友请求",
                    nickname + " 请求添加您为好友",
                    request.getId(),
                    null);
            return request;
        }, "操作过于频繁，请稍后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFriendRequest(Long toUid, HandleFriendRequestDTO dto) {
        Long requestId = dto.getRequestId();
        Integer action = dto.getAction();
        if (!Integer.valueOf(ACTION_ACCEPT).equals(action) && !Integer.valueOf(ACTION_REJECT).equals(action)) {
            throw new BusinessException("处理动作不合法");
        }

        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("好友请求不存在");
        }
        if (!request.getToUid().equals(toUid)) {
            throw new BusinessException("无权处理该请求");
        }
        if (!FriendRequestStatus.PENDING.getCode().equals(request.getStatus())) {
            throw new BusinessException("该请求已处理，请勿重复操作");
        }

        String lockKey = "friend:handle:" + requestId;
        // 建立 redis分布式锁防止并非发操作
        lockHelper.tryLock(lockKey, 3, 10, () -> {
            // 双重检查
            FriendRequest fresh = friendRequestMapper.selectById(requestId);
            if (fresh == null || !FriendRequestStatus.PENDING.getCode().equals(fresh.getStatus())) {
                throw new BusinessException("该请求已处理，请勿重复操作");
            }
            if (Integer.valueOf(ACTION_ACCEPT).equals(action)) {
                acceptRequest(fresh, toUid, dto.getReplyMsg());
            } else {
                rejectRequest(fresh, toUid, dto.getReplyMsg());
            }
        }, "请求正在处理中，请稍后重试");
    }

    private void acceptRequest(FriendRequest request, Long operatorUid, String replyMsg) {
        Long fromUid = request.getFromUid();
        Long toUid = request.getToUid();
        LocalDateTime now = LocalDateTime.now();

        // 插入双向好友关系
        createFriendRelation(fromUid, toUid, now);
        createFriendRelation(toUid, fromUid, now);

        // 更新请求状态
        request.setStatus(FriendRequestStatus.ACCEPTED.getCode());
        request.setReplyMsg(replyMsg);
        request.setUpdateTime(now);
        friendRequestMapper.updateById(request);

        // 给发送者发送同意通知
        User operator = userService.getById(operatorUid);
        String nickname = operator == null ? "用户" + operatorUid : operator.getNickname();
        notificationService.notifyUser(fromUid, NotificationType.FRIEND_ACCEPTED,
                "好友请求已同意",
                nickname + " 已同意您的好友请求",
                request.getId(),
                null);
    }

    private void rejectRequest(FriendRequest request, Long operatorUid, String replyMsg) {
        LocalDateTime now = LocalDateTime.now();
        request.setStatus(FriendRequestStatus.REJECTED.getCode());
        request.setReplyMsg(replyMsg);
        request.setUpdateTime(now);
        friendRequestMapper.updateById(request);

        User operator = userService.getById(operatorUid);
        String nickname = operator == null ? "用户" + operatorUid : operator.getNickname();
        notificationService.notifyUser(request.getFromUid(), NotificationType.SYSTEM,
                "好友请求被拒绝",
                nickname + " 拒绝了您的好友请求",
                request.getId(),
                null);
    }

    private void createFriendRelation(Long uId, Long friendId, LocalDateTime now) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUId, uId).eq(Friend::getFriendId, friendId);
        if (friendMapper.selectCount(wrapper) > 0) {
            return;
        }
        Friend friend = new Friend();
        friend.setUId(uId);
        friend.setFriendId(friendId);
        friend.setStatus(FRIEND_STATUS_NORMAL);
        friend.setCreateTime(now);
        friend.setUpdateTime(now);
        friendMapper.insert(friend);
    }

    private boolean isFriend(Long uId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUId, uId)
                .eq(Friend::getFriendId, friendId)
                .eq(Friend::getStatus, FRIEND_STATUS_NORMAL);
        return friendMapper.selectCount(wrapper) > 0;
    }

    private FriendRequest getPendingRequest(Long fromUid, Long toUid) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getFromUid, fromUid)
                .eq(FriendRequest::getToUid, toUid)
                .eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode())
                .last("limit 1");
        return friendRequestMapper.selectOne(wrapper);
    }

    @Override
    public List<FriendRequestVO> listReceivedRequests(Long toUid, Integer status) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getToUid, toUid);
        if (status != null) {
            wrapper.eq(FriendRequest::getStatus, status);
        }
        wrapper.orderByDesc(FriendRequest::getCreateTime);
        List<FriendRequest> list = friendRequestMapper.selectList(wrapper);
        return convertToRequestVo(list);
    }

    @Override
    public List<FriendRequestVO> listSentRequests(Long fromUid, Integer status) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getFromUid, fromUid);
        if (status != null) {
            wrapper.eq(FriendRequest::getStatus, status);
        }
        wrapper.orderByDesc(FriendRequest::getCreateTime);
        List<FriendRequest> list = friendRequestMapper.selectList(wrapper);
        return convertToRequestVo(list);
    }

    @Override
    public List<FriendVO> listFriends(Long uId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUId, uId)
                .eq(Friend::getStatus, FRIEND_STATUS_NORMAL)
                .orderByDesc(Friend::getCreateTime);
        List<Friend> friends = friendMapper.selectList(wrapper);
        if (friends.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> friendIds = friends.stream().map(Friend::getFriendId).distinct().toList();
        Map<Long, User> userMap = userService.listByIds(friendIds).stream()
                .collect(Collectors.toMap(User::getUId, Function.identity(), (a, b) -> a));

        return friends.stream().map(f -> {
            FriendVO vo = new FriendVO();
            vo.setId(f.getId());
            vo.setFriendId(f.getFriendId());
            User user = userMap.get(f.getFriendId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            } else {
                vo.setNickname("用户" + f.getFriendId());
            }
            vo.setStatus(f.getStatus());
            vo.setCreateTime(f.getCreateTime());
            return vo;
        }).toList();
    }

    private List<FriendRequestVO> convertToRequestVo(List<FriendRequest> list) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> fromUids = list.stream().map(FriendRequest::getFromUid).distinct().toList();
        Map<Long, User> userMap = userService.listByIds(fromUids).stream()
                .collect(Collectors.toMap(User::getUId, Function.identity(), (a, b) -> a));

        return list.stream().map(req -> {
            FriendRequestVO vo = new FriendRequestVO();
            vo.setId(req.getId());
            vo.setFromUid(req.getFromUid());
            vo.setToUid(req.getToUid());
            vo.setStatus(req.getStatus());
            FriendRequestStatus statusEnum = FriendRequestStatus.of(req.getStatus());
            vo.setStatusDesc(statusEnum == null ? "" : statusEnum.getDesc());
            vo.setApplyMsg(req.getApplyMsg());
            vo.setReplyMsg(req.getReplyMsg());
            vo.setCreateTime(req.getCreateTime());
            User user = userMap.get(req.getFromUid());
            if (user != null) {
                vo.setFromNickname(user.getNickname());
                vo.setFromAvatar(user.getAvatar());
            } else {
                vo.setFromNickname("用户" + req.getFromUid());
            }
            return vo;
        }).toList();
    }

    private String buildPairLockKey(String prefix, Long uid1, Long uid2) {
        long min = Math.min(uid1, uid2);
        long max = Math.max(uid1, uid2);
        return prefix + ":" + min + ":" + max;
    }
}
