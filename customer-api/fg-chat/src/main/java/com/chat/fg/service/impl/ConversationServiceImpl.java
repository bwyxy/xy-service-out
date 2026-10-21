package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.common.constant.Constants;
import com.xy.common.entity.SysImUser;
import com.xy.common.entity.chat.ConversationSetting;
import com.xy.common.enums.OfficialAccount;
import com.xy.system.mapper.SysImUserMapper;
import com.xy.system.mapper.UserMapper;
import com.chat.fg.mapper.ConversationSettingMapper;
import com.chat.fg.pojo.dto.ConversationChannelDTO;
import com.chat.fg.pojo.dto.ConversationDTO;
import com.chat.fg.pojo.dto.ConversationTopDTO;
import com.chat.fg.pojo.vo.UserConversationVO;
import com.chat.fg.service.IConversationService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.ConversationRequest;
import com.xy.common.client.dto.request.MsgRequest;
import com.xy.common.client.entity.Message;
import com.xy.common.client.vo.ConversationVO;
import com.xy.common.client.vo.MessageVO;
import com.xy.common.core.redis.RedisCache;
import com.xy.common.entity.chat.Group;
import com.xy.common.entity.User;
import com.xy.common.utils.ChannelIdUtils;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.GroupMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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

    @Autowired
    private RedisCache redisCache;

    /** 系统账号 UID 缓存 Key：变更极少，缓存后避免每次拉会话列表都穿透到悟空 IM */
    private static final String SYSTEM_UIDS_CACHE_KEY = "chat:system:uids";

    /** 系统账号 UID 缓存时长（分钟） */
    private static final long SYSTEM_UIDS_CACHE_MINUTES = 5;

    /**
     * 获取会话列表
     * */
    @Override
    public List<UserConversationVO> getConversations(ConversationDTO conversationDTO) {

        ConversationRequest conversationRequest = getConversationRequest(conversationDTO);
        // 获取系统用户
        List<String> systemUids = getSystemUids();
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

        // 已解散的群聊不再出现在会话列表中
        Set<Long> disbandedGroupIds = groupMap.values().stream()
                .filter(group -> Integer.valueOf(1).equals(group.getDisband()))
                .map(Group::getGroupId)
                .collect(Collectors.toSet());

        List<UserConversationVO> result = new ArrayList<>();

        for (ConversationVO conv : conversations) {
            ConversationSetting setting = settingMap.get(getChannelKey(conv.getChannelId(), conv.getChannelType()));

            // 已删除的会话在收到新消息前不再展示
            if (isConversationDeleted(setting, conv.getLastMsgSeq())) {
                continue;
            }

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
            vo.setTop(setting != null ? setting.getTop() : 0);

            // 已清空聊天记录的会话不再返回最后一条消息预览
            if (isMessageCleared(setting, conv.getLastMsgSeq())) {
                vo.setRecents(new ArrayList<>());
            }

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
                Long groupId = parseGroupId(conv.getChannelId());

                // 群聊已解散，跳过该会话
                if (groupId != null && disbandedGroupIds.contains(groupId)) {
                    continue;
                }

                Group group = groupId != null ? groupMap.get(groupId) : null;

                if (group != null) {
                    vo.setName(group.getName());
                    vo.setAvatar(group.getAvatar());
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
    @Transactional(rollbackFor = Exception.class)
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

            try {
                conversationSettingMapper.insert(setting);
            } catch (DuplicateKeyException e) {
                // 并发首次置顶同一会话时，唯一索引兜底后回查再更新
                setting = conversationSettingMapper.selectOne(queryWrapper);
                setting.setTop(top);
                conversationSettingMapper.updateById(setting);
            }

            return;
        }

        setting.setTop(top);

        conversationSettingMapper.updateById(setting);
    }

    /**
     * 删除聊天记录
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearMessages(ConversationChannelDTO conversationChannelDTO) {
        saveClearMarker(conversationChannelDTO, false);
    }

    /**
     * 删除会话
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(ConversationChannelDTO conversationChannelDTO) {
        saveClearMarker(conversationChannelDTO, true);
    }

    /**
     * 保存删除标记
     *
     * <p>仅对本用户生效：记录删除时刻频道的最新消息序号，
     * 之后拉取消息时隐藏不大于该序号的消息；删除会话额外在会话列表中隐藏该会话。</p>
     * */
    private void saveClearMarker(ConversationChannelDTO conversationChannelDTO, boolean deleteConversation) {
        Long uId = SecurityUtils.getUId();
        long latestSeq = getLatestMessageSeq(conversationChannelDTO.getChannelId(),
                conversationChannelDTO.getChannelType());

        ConversationSetting setting = getOrCreateSetting(uId,
                conversationChannelDTO.getChannelId(), conversationChannelDTO.getChannelType());

        if (latestSeq > 0) {
            setting.setClearedSeq(latestSeq);

            if (deleteConversation) {
                setting.setDeletedSeq(latestSeq);
            }
        } else if (deleteConversation && setting.getDeletedSeq() == null) {
            // 频道还没有消息时，标记为已删除即可
            setting.setDeletedSeq(0L);
        }

        conversationSettingMapper.updateById(setting);
    }

    /**
     * 查询当前用户的会话个人设置，不存在则初始化
     * */
    private ConversationSetting getOrCreateSetting(Long uId, String channelId, Integer channelType) {
        LambdaQueryWrapper<ConversationSetting> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ConversationSetting::getUId, uId)
                .eq(ConversationSetting::getChannelId, channelId)
                .eq(ConversationSetting::getChannelType, channelType);

        ConversationSetting setting = conversationSettingMapper.selectOne(queryWrapper);

        if (setting == null) {
            setting = new ConversationSetting();
            setting.setUId(uId);
            setting.setChannelId(channelId);
            setting.setChannelType(channelType);
            setting.setClearedSeq(0L);
            setting.setDeletedSeq(0L);

            try {
                conversationSettingMapper.insert(setting);
            } catch (DuplicateKeyException e) {
                // 并发首次操作同一会话时会同时插入，靠 (u_id, channel_id, channel_type) 唯一索引兜底后回查
                setting = conversationSettingMapper.selectOne(queryWrapper);
            }
        }

        return setting;
    }

    /**
     * 查询频道最新的消息序号，无消息时返回 0
     * */
    private long getLatestMessageSeq(String channelId, Integer channelType) {
        MsgRequest msgRequest = new MsgRequest();

        msgRequest.setLoginUid(String.valueOf(SecurityUtils.getUId()));
        msgRequest.setChannelId(channelId);
        msgRequest.setChannelType(channelType);
        msgRequest.setStartMessageSeq(0);
        msgRequest.setEndMessageSeq(0);
        msgRequest.setLimit(1);
        msgRequest.setPullMode(0);

        try {
            MessageVO messageVO = wuKongClient.messageSync(msgRequest);

            if (messageVO == null || messageVO.getMessages() == null || messageVO.getMessages().isEmpty()) {
                return 0L;
            }

            return messageVO.getMessages().stream()
                    .map(Message::getMessageSeq)
                    .filter(Objects::nonNull)
                    .max(Long::compareTo)
                    .orElse(0L);
        } catch (Exception e) {
            log.warn("查询频道最新消息序号失败, channelId={}, channelType={}", channelId, channelType, e);
            return 0L;
        }
    }

    /**
     * 会话是否已被删除且尚无新消息
     * */
    private boolean isConversationDeleted(ConversationSetting setting, Integer lastMsgSeq) {
        if (setting == null || setting.getDeletedSeq() == null || setting.getDeletedSeq() <= 0) {
            return false;
        }

        return (lastMsgSeq == null ? 0 : lastMsgSeq) <= setting.getDeletedSeq();
    }

    /**
     * 会话的聊天记录是否已被清空
     * */
    private boolean isMessageCleared(ConversationSetting setting, Integer lastMsgSeq) {
        if (setting == null || setting.getClearedSeq() == null || setting.getClearedSeq() <= 0) {
            return false;
        }

        return (lastMsgSeq == null ? 0 : lastMsgSeq) <= setting.getClearedSeq();
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

    /**
     * 获取系统账号 UID 列表（带短 TTL 缓存）
     *
     * <p>系统账号由悟空 IM 维护且变更极少，会话列表接口高频调用，
     * 缓存后避免每次请求都多一次 IM HTTP 调用。</p>
     * */
    private List<String> getSystemUids() {
        List<String> cached = redisCache.getCacheList(SYSTEM_UIDS_CACHE_KEY);

        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<String> systemUids = wuKongClient.getSystemUids();

        if (systemUids == null || systemUids.isEmpty()) {
            // 空结果不写缓存，避免 IM 异常时把空列表固化下来
            return new ArrayList<>();
        }

        redisCache.setCacheList(SYSTEM_UIDS_CACHE_KEY, systemUids);
        redisCache.expire(SYSTEM_UIDS_CACHE_KEY, SYSTEM_UIDS_CACHE_MINUTES, TimeUnit.MINUTES);

        return systemUids;
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

    /**
     * 从频道 ID 中解析群组 ID（形如 g_123）
     * */
    private Long parseGroupId(String channelId) {
        return ChannelIdUtils.parseGroupId(channelId);
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
        List<Long> groupIds = gIds.stream()
                .map(ChannelIdUtils::parseGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (groupIds.isEmpty()) return new HashMap<>();

        List<Group> groups = groupMapper.selectBatchIds(groupIds);

        return groups.stream().collect(Collectors.toMap(Group::getGroupId, Function.identity()));
    }

    private void setDefaultUserInfo(UserConversationVO userConversationVO) {
        userConversationVO.setName("飞鸽用户");
        userConversationVO.setAvatar("");
    }
}
