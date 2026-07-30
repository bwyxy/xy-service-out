package com.chat.fg.service.impl;

import com.chat.fg.mapper.UserMapper;
import com.chat.fg.pojo.dto.ConversationDTO;
import com.chat.fg.pojo.vo.UserConversationVO;
import com.chat.fg.service.IConversationService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.ConversationRequest;
import com.xy.common.client.vo.ConversationVO;
import com.xy.common.entity.Group;
import com.xy.common.entity.User;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.GroupMapper;
import com.xy.system.service.impl.GroupServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements IConversationService {

    @Autowired
    private WuKongClient wuKongClient;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GroupMapper groupMapper;

    /**
     * 获取会话列表
     * */
    @Override
    public List<UserConversationVO> getConversations(ConversationDTO conversationDTO) {

        ConversationRequest conversationRequest = getConversationRequest(conversationDTO);
        // 获取系统用户
        List<String> systemUids = wuKongClient.getSystemUids();
        // 获取会话列表
        List<ConversationVO> conversations = wuKongClient.syncConversation(conversationRequest);

        if (!StringUtils.isNotEmpty(conversations)) {
            return new ArrayList<>();
        }

        List<String> personalIds = new ArrayList<>();
        List<String> groupIds = new ArrayList<>();
        Map<String, Boolean> isSystemMap = new HashMap<>();

        for (ConversationVO conv : conversations) {
            if (conv.getChannelType() == 1) {
                String channelId = conv.getChannelId();
                if (systemUids.contains(channelId)) {
                    isSystemMap.put(channelId, true);
                } else {
                    personalIds.add(conv.getChannelId());
                    isSystemMap.put(channelId, false);
                }
            } else if (conv.getChannelType() == 2) {
                groupIds.add(conv.getChannelId());
            }
        }

        // 获取信息
        Map<Long, User> userMap = queryUsers(personalIds);
        Map<Long, Group> groupMap = queryGroups(groupIds);

        List<UserConversationVO> result = new ArrayList<>();

        for (ConversationVO conv : conversations) {
            UserConversationVO vo = new UserConversationVO();

            vo.setChannelId(conv.getChannelId());
            vo.setChannelType(conv.getChannelType());
            vo.setUnread(conv.getUnread());
            vo.setTimestamp(conv.getTimestamp());
            vo.setLastClientMsgNo(conv.getLastClientMsgNo());
            vo.setLastMsgSeq(conv.getLastMsgSeq());
            vo.setOffsetMsgSeq(conv.getOffsetMsgSeq());
            vo.setReadedToMsgSeq(conv.getReadedToMsgSeq());
            vo.setVersion(conv.getVersion());
            vo.setRecents(conv.getRecents());

            if (conv.getChannelType() == 1) {
                String channelId = conv.getChannelId();
                if (isSystemMap.getOrDefault(channelId, false)) {
                    // 系统账号
                    vo.setName(getSystemName(channelId));
                    vo.setAvatar(getSystemAvatar(channelId));
                    vo.setSystem(true);
                } else {
                    // 普通用户
                    Long userId = parseLongSafe(channelId);
                    if (userId != null) {
                        User user = userMap.get(userId);
                        if (user != null) {
                            vo.setName(user.getNickname());
                            vo.setAvatar(user.getAvatar());
                        } else {
                            setDefaultUserInfo(vo);
                        }
                    } else {
                        setDefaultUserInfo(vo);
                    }
                }
            } else if (conv.getChannelType() == 2) {
                Long groupId = parseLongSafe(conv.getChannelId());
                if (groupId != null) {
                    Group group = groupMap.get(groupId);
                    if (group != null) {
                        vo.setName(group.getName());
                        vo.setAvatar(group.getAvatar());
                    } else {
                        vo.setName("未更名群组");
                        vo.setAvatar("");
                    }
                } else {
                    vo.setName("未更名群组");
                    vo.setAvatar("");
                }
            }

            result.add(vo);
        }

        return result;
    }

    /** 解析获取会话列表的参数 */
    private ConversationRequest getConversationRequest(ConversationDTO conversationDTO) {
        Long uId = SecurityUtils.getUId();

        ConversationRequest conversationRequest = new ConversationRequest();

        conversationRequest.setUid(String.valueOf(uId));
        conversationRequest.setVersion(conversationDTO.getVersion());
        conversationRequest.setLastMsgSeqs(conversationDTO.getLastMsgSeqs());
        conversationRequest.setMsgCount(conversationDTO.getMsgCount());
        conversationRequest.setOnlyUnread(conversationDTO.getOnlyUnread());
        conversationRequest.setExcludeChannelTypes(conversationDTO.getExcludeChannelTypes());

        return conversationRequest;
    }

    private Long parseLongSafe(String str) {
        if (str == null || str.isEmpty()) return null;

        if (!str.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 转换 uIds 类型
     * */
    private List<Long> parseLongUids(List<String> uIds) {
        if (uIds.isEmpty()) return new ArrayList<>();

        return uIds.stream()
                .map(this::parseLongSafe)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 批量查询用户
     * */
    private Map<Long, User> queryUsers(List<String> uIds) {
        List<Long> userIds = parseLongUids(uIds);

        if (userIds.isEmpty()) return new HashMap<>();

        List<User> users = userMapper.selectBatchIds(userIds);

        return users.stream().collect(Collectors.toMap(User::getUId, Function.identity()));
    }

    /**
     * 批量查询群组
     * */
    private Map<Long, Group> queryGroups(List<String> gIds) {
        List<Long> groupIds = parseLongUids(gIds);

        if (groupIds.isEmpty()) return new HashMap<>();

        List<Group> groups = groupMapper.selectBatchIds(groupIds);

        return groups.stream().collect(Collectors.toMap(Group::getGroupId, Function.identity()));
    }

    private String getSystemName(String channelId) {
        if ("system_admin".equals(channelId)) return "飞鸽团队";
        if ("fg_ai".equals(channelId)) return "飞鸽AI助手";
        return "系统消息";
    }

    private String getSystemAvatar(String channelId) {
        return "";
    }

    private void setDefaultUserInfo(UserConversationVO userConversationVO) {
        userConversationVO.setName("飞鸽用户");
        userConversationVO.setAvatar("");
    }
}
