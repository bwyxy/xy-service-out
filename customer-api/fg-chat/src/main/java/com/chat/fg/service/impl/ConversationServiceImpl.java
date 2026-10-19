package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.common.constant.Constants;
import com.xy.common.entity.SysImUser;
import com.xy.common.entity.chat.ConversationSetting;
import com.xy.common.enums.OfficialAccount;
import com.xy.system.mapper.SysImUserMapper;
import com.xy.system.mapper.UserMapper;
import com.chat.fg.mapper.ConversationSettingMapper;
import com.chat.fg.pojo.dto.ConversationDTO;
import com.chat.fg.pojo.dto.ConversationTopDTO;
import com.chat.fg.pojo.vo.UserConversationVO;
import com.chat.fg.service.IConversationService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.ConversationRequest;
import com.xy.common.client.vo.ConversationVO;
import com.xy.common.entity.chat.Group;
import com.xy.common.entity.User;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.GroupMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConversationServiceImpl implements IConversationService {

    @Autowired
    private WuKongClient wuKongClient;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private SysImUserMapper sysImUserMapper;

    @Autowired
    private ConversationSettingMapper conversationSettingMapper;

    /**
     * 获取会话列表
     * */
    @Override
    public List<UserConversationVO> getConversations(ConversationDTO conversationDTO) {

        ConversationRequest conversationRequest = getConversationRequest(conversationDTO);
        // 获取系统用户
        List<String> systemUids = wuKongClient.getSystemUids();
        // 获取会话列表
        List<ConversationVO> conversations = getWKConversations(conversationRequest);

        if (!StringUtils.isNotEmpty(conversations)) {
            return new ArrayList<>();
        }

        // 当前用户的会话个人设置（置顶等）
        Map<String, ConversationSetting> settingMap = queryConversationSettings(conversations);

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
        Map<String, SysImUser> systemUserMap = querySystemUsers(systemUids);
        Pattern pattern = Pattern.compile("\\d+");

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

            // 置顶状态来自会话个人设置表，未设置过则为未置顶
            ConversationSetting setting = settingMap.get(getChannelKey(conv.getChannelId(), conv.getChannelType()));
            vo.setTop(setting != null ? setting.getTop() : 0);

            if (conv.getChannelType() == 1) {
                String channelId = conv.getChannelId();
                if (isSystemMap.getOrDefault(channelId, false)) {
                    // 系统账号
                    SysImUser sysImUser = systemUserMap.get(channelId);
                    if (sysImUser != null) {
                        vo.setName(sysImUser.getNickname());
                        vo.setAvatar(sysImUser.getAvatar());
                    } else {
                        vo.setName("系统账号");
                        vo.setAvatar("");
                    }
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
                Matcher matcher = pattern.matcher(conv.getChannelId());
                if (matcher.find()) {
                    Long groupId = parseLongSafe(matcher.group());
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

        // 置顶会话优先，其余按最后消息时间倒序（时间相同按频道 ID 稳定排序）
        result.sort(Comparator
                .comparing(UserConversationVO::getTop, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UserConversationVO::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UserConversationVO::getChannelId));

        return paginate(result, conversationDTO);
    }

    /**
     * 设置/取消会话置顶
     *
     * <p>会话列表来自悟空 IM，本地只用 t_conversation_setting 记录置顶等个人设置。</p>
     * */
    @Override
    public void updateTop(ConversationTopDTO conversationTopDTO) {
        Long uId = SecurityUtils.getUId();
        int top = conversationTopDTO.getTop() != null && conversationTopDTO.getTop() == 1 ? 1 : 0;

        LambdaQueryWrapper<ConversationSetting> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ConversationSetting::getUId, uId)
                .eq(ConversationSetting::getChannelId, conversationTopDTO.getChannelId())
                .eq(ConversationSetting::getChannelType, conversationTopDTO.getChannelType());

        ConversationSetting setting = conversationSettingMapper.selectOne(queryWrapper);

        if (setting == null) {
            // 没有其他设置项且为取消置顶时，无需新增记录
            if (top == 0) {
                return;
            }

            setting = new ConversationSetting();
            setting.setUId(uId);
            setting.setChannelId(conversationTopDTO.getChannelId());
            setting.setChannelType(conversationTopDTO.getChannelType());
            setting.setTop(top);

            conversationSettingMapper.insert(setting);

            return;
        }

        setting.setTop(top);

        conversationSettingMapper.updateById(setting);
    }

    /**
     * 批量查询当前用户的会话个人设置
     * */
    private Map<String, ConversationSetting> queryConversationSettings(List<ConversationVO> conversations) {
        List<String> channelIds = conversations.stream()
                .map(ConversationVO::getChannelId)
                .distinct()
                .collect(Collectors.toList());

        if (channelIds.isEmpty()) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<ConversationSetting> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ConversationSetting::getUId, SecurityUtils.getUId())
                .in(ConversationSetting::getChannelId, channelIds);

        return conversationSettingMapper.selectList(queryWrapper).stream()
                .collect(Collectors.toMap(
                        setting -> getChannelKey(setting.getChannelId(), setting.getChannelType()),
                        Function.identity(),
                        (existing, replacement) -> existing));
    }

    /** 会话唯一标识：频道 ID + 频道类型 */
    private String getChannelKey(String channelId, Integer channelType) {
        return channelId + "_" + channelType;
    }

    /**
     * 对会话列表分页
     *
     * <p>悟空 IM conversation/sync 本身不支持分页，这里在应用层对全量结果切片。
     * pageSize<=0 时不分页，保持向后兼容。</p>
     * */
    private List<UserConversationVO> paginate(List<UserConversationVO> list, ConversationDTO conversationDTO) {
        Integer page = conversationDTO.getPage();
        Integer pageSize = conversationDTO.getPageSize();

        if (page == null || pageSize == null || pageSize <= 0) {
            return list;
        }

        int from = Math.max(page - 1, 0) * pageSize;

        if (from >= list.size()) {
            return new ArrayList<>();
        }

        int to = Math.min(from + pageSize, list.size());

        return new ArrayList<>(list.subList(from, to));
    }

    /** 获取悟空 IM会话列表 */
    private List<ConversationVO> getWKConversations(ConversationRequest request) {
        // 获取会话列表
        List<ConversationVO> conversations = wuKongClient.syncConversation(request);
        String systemNotice = OfficialAccount.systemNotice.getCode();

        return conversations.stream()
                .filter(conv -> !systemNotice.equals(conv.getChannelId()))
                .collect(Collectors.toList());
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
     * 批量查询系统用户
     * */
    private Map<String, SysImUser> querySystemUsers(List<String> uIds) {
        if (uIds.isEmpty()) return new HashMap<>();

        LambdaQueryWrapper<SysImUser> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.in(SysImUser::getWkImKey, uIds);

        List<SysImUser> sysImUsers = sysImUserMapper.selectList(queryWrapper);

        return sysImUsers.stream().collect(Collectors.toMap(SysImUser::getWkImKey, Function.identity()));
    }

    /**
     * 批量查询群组
     * */
    private Map<Long, Group> queryGroups(List<String> gIds) {
        List<String> ids = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\d+");

        for (String gId : gIds) {
            Matcher matcher = pattern.matcher(gId);

            if (matcher.find()) {
                ids.add(matcher.group());
            }
        }
        log.warn("ids:{}", ids);

        List<Long> groupIds = parseLongUids(ids);

        if (groupIds.isEmpty()) return new HashMap<>();

        List<Group> groups = groupMapper.selectBatchIds(groupIds);

        return groups.stream().collect(Collectors.toMap(Group::getGroupId, Function.identity()));
    }

    private void setDefaultUserInfo(UserConversationVO userConversationVO) {
        userConversationVO.setName("飞鸽用户");
        userConversationVO.setAvatar("");
    }
}
