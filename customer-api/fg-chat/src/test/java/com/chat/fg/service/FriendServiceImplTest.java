package com.chat.fg.service;

import com.chat.fg.mapper.FriendMapper;
import com.chat.fg.mapper.FriendRequestMapper;
import com.chat.fg.pojo.dto.FriendRequestDTO;
import com.chat.fg.pojo.dto.HandleFriendRequestDTO;
import com.chat.fg.pojo.vo.FriendRequestVO;
import com.chat.fg.pojo.vo.FriendVO;
import com.chat.fg.service.impl.FriendServiceImpl;
import com.chat.fg.service.impl.UserServiceImpl;
import com.chat.fg.utils.DistributedLockHelper;
import com.xy.common.entity.Friend;
import com.xy.common.entity.FriendRequest;
import com.xy.common.entity.User;
import com.xy.common.enums.FriendRequestStatus;
import com.xy.common.enums.NotificationType;
import com.xy.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 好友服务单元测试
 * */
@ExtendWith(MockitoExtension.class)
class FriendServiceImplTest {

    @Mock
    private FriendRequestMapper friendRequestMapper;

    @Mock
    private FriendMapper friendMapper;

    @Mock
    private UserServiceImpl userService;

    @Mock
    private com.xy.system.service.INotificationService notificationService;

    @Mock
    private DistributedLockHelper lockHelper;

    @InjectMocks
    private FriendServiceImpl friendService;

    private void stubLockForSupplier() {
        when(lockHelper.tryLock(anyString(), anyLong(), anyLong(), any(java.util.function.Supplier.class), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(3, java.util.function.Supplier.class).get());
    }

    private void stubLockForRunnable() {
        doAnswer(invocation -> {
            invocation.getArgument(3, Runnable.class).run();
            return null;
        }).when(lockHelper).tryLock(anyString(), anyLong(), anyLong(), any(Runnable.class), anyString());
    }

    @Test
    void searchUsers_shouldReturnMatchedUsers() {
        User user = new User();
        user.setUId(10002L);
        user.setNickname("用户B");
        when(userService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(user));

        List<User> result = friendService.searchUsers(10001L, "用户B");

        assertEquals(1, result.size());
        assertEquals(10002L, result.get(0).getUId());
    }

    @Test
    void sendFriendRequest_shouldThrowWhenTargetIsSelf() {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setToUid(10001L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> friendService.sendFriendRequest(10001L, dto));
        assertEquals("不能添加自己为好友", exception.getMessage());
    }

    @Test
    void sendFriendRequest_shouldThrowWhenAlreadyFriend() {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setToUid(10002L);

        User target = new User();
        target.setUId(10002L);
        target.setStatus("0");
        when(userService.getById(10002L)).thenReturn(target);
        when(friendMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> friendService.sendFriendRequest(10001L, dto));
        assertEquals("对方已经是您的好友", exception.getMessage());
    }

    @Test
    void sendFriendRequest_shouldCreateRequest() {
        stubLockForSupplier();

        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setToUid(10002L);
        dto.setApplyMsg("你好");

        User target = new User();
        target.setUId(10002L);
        target.setStatus("0");
        User fromUser = new User();
        fromUser.setUId(10001L);
        fromUser.setNickname("用户A");

        when(userService.getById(10002L)).thenReturn(target);
        when(friendMapper.selectCount(any())).thenReturn(0L);
        when(friendRequestMapper.selectOne(any())).thenReturn(null);
        when(userService.getById(10001L)).thenReturn(fromUser);
        when(friendRequestMapper.insert(any(FriendRequest.class))).thenAnswer(inv -> {
            FriendRequest req = inv.getArgument(0);
            req.setId(1L);
            return 1;
        });
        when(notificationService.notifyUser(eq(10002L), eq(NotificationType.FRIEND_REQUEST), anyString(), anyString(), any(), isNull()))
                .thenReturn(new com.xy.common.entity.Notification());

        FriendRequest result = friendService.sendFriendRequest(10001L, dto);

        assertNotNull(result);
        assertEquals(FriendRequestStatus.PENDING.getCode(), result.getStatus());
        assertEquals(10001L, result.getFromUid());
        assertEquals(10002L, result.getToUid());
        verify(friendRequestMapper, times(1)).insert(any(FriendRequest.class));
    }

    @Test
    void handleFriendRequest_shouldAcceptAndCreateFriendRelation() {
        stubLockForRunnable();

        HandleFriendRequestDTO dto = new HandleFriendRequestDTO();
        dto.setRequestId(1L);
        dto.setAction(1);

        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setFromUid(10001L);
        request.setToUid(10002L);
        request.setStatus(FriendRequestStatus.PENDING.getCode());

        when(friendRequestMapper.selectById(1L)).thenReturn(request);
        when(friendMapper.selectCount(any())).thenReturn(0L);
        when(friendMapper.insert(any(Friend.class))).thenReturn(1);
        when(userService.getById(10002L)).thenReturn(createUser(10002L, "用户B"));
        when(notificationService.notifyUser(eq(10001L), eq(NotificationType.FRIEND_ACCEPTED), anyString(), anyString(), any(), isNull()))
                .thenReturn(new com.xy.common.entity.Notification());

        friendService.handleFriendRequest(10002L, dto);

        assertEquals(FriendRequestStatus.ACCEPTED.getCode(), request.getStatus());
        verify(friendMapper, times(2)).insert(any(Friend.class));
    }

    @Test
    void handleFriendRequest_shouldThrowWhenAlreadyHandled() {
        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setFromUid(10001L);
        request.setToUid(10002L);
        request.setStatus(FriendRequestStatus.ACCEPTED.getCode());

        when(friendRequestMapper.selectById(1L)).thenReturn(request);

        HandleFriendRequestDTO dto = new HandleFriendRequestDTO();
        dto.setRequestId(1L);
        dto.setAction(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> friendService.handleFriendRequest(10002L, dto));
        assertEquals("该请求已处理，请勿重复操作", exception.getMessage());
    }

    @Test
    void listFriends_shouldReturnFriendsWithUserInfo() {
        Friend friend = new Friend();
        friend.setId(1L);
        friend.setUId(10001L);
        friend.setFriendId(10002L);
        friend.setStatus("0");

        when(friendMapper.selectList(any())).thenReturn(List.of(friend));
        when(userService.listByIds(List.of(10002L))).thenReturn(List.of(createUser(10002L, "用户B")));

        List<FriendVO> result = friendService.listFriends(10001L);

        assertEquals(1, result.size());
        assertEquals("用户B", result.get(0).getNickname());
        assertEquals(10002L, result.get(0).getFriendId());
    }

    @Test
    void listReceivedRequests_shouldFilterByStatus() {
        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setFromUid(10001L);
        request.setToUid(10002L);
        request.setStatus(FriendRequestStatus.PENDING.getCode());

        when(friendRequestMapper.selectList(any())).thenReturn(List.of(request));
        when(userService.listByIds(List.of(10001L))).thenReturn(List.of(createUser(10001L, "用户A")));

        List<FriendRequestVO> result = friendService.listReceivedRequests(10002L, FriendRequestStatus.PENDING.getCode());

        assertEquals(1, result.size());
        assertEquals("用户A", result.get(0).getFromNickname());
    }

    private User createUser(Long uId, String nickname) {
        User user = new User();
        user.setUId(uId);
        user.setNickname(nickname);
        user.setStatus("0");
        return user;
    }
}
