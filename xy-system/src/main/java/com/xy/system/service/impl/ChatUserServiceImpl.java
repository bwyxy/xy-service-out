package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.User;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import com.xy.common.vo.chat.ChatUserQuery;
import com.xy.common.vo.chat.ChatUserVO;
import com.xy.system.mapper.UserMapper;
import com.xy.system.service.IChatSessionService;
import com.xy.system.service.IChatUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 客户端用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUserServiceImpl implements IChatUserService {

    /** 在线状态过滤时的单批扫描条数 */
    private static final int ONLINE_SCAN_BATCH = 200;

    /** 在线状态过滤时的最大扫描条数，避免全表逐条比对 Redis */
    private static final int ONLINE_SCAN_MAX = 5000;

    /** 用户状态：禁用 */
    private static final String STATUS_DISABLED = "1";

    /** 用户状态：正常 */
    private static final String STATUS_NORMAL = "0";

    /** 未绑定聊天应用 */
    private static final String UNBIND_CHAT = "0";

    /** 微信用户默认昵称前缀 */
    private static final String DEFAULT_NICKNAME_PREFIX = "微信用户";

    private final UserMapper userMapper;
    private final IChatSessionService chatSessionService;

    @Override
    public PageResult<ChatUserVO> selectChatUserPage(ChatUserQuery query) {
        int pageNum = query.getPageNum() <= 0 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() <= 0 ? 10 : query.getPageSize();

        LambdaQueryWrapper<User> wrapper = buildQueryWrapper(query);

        if (StringUtils.isEmpty(query.getOnline())) {
            Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
            return PageResult.of(toVOList(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
        }

        return pageByOnline(query.getOnline(), pageNum, pageSize, wrapper);
    }

    @Override
    public ChatUserVO getChatUserDetail(Long uId) {
        if (uId == null) {
            throw new BusinessException("用户 UID 不能为空");
        }
        User user = userMapper.selectById(uId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVOList(List.of(user)).get(0);
    }

    @Override
    public void updateStatus(Long uId, String status) {
        if (uId == null || StringUtils.isEmpty(status)) {
            throw new BusinessException("参数不完整");
        }
        if (!"0".equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException("非法的用户状态");
        }

        User user = userMapper.selectById(uId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        User update = new User();
        update.setUId(uId);
        update.setStatus(status);
        userMapper.updateById(update);

        // 禁用后立即清退全部在线设备，否则旧 token 到期前仍可正常收发消息
        if (STATUS_DISABLED.equals(status)) {
            int count = chatSessionService.forceLogoutAll(uId, "您的账号已被管理员禁用，请联系管理员处理");
            log.info("禁用用户并强制下线, uId={}, sessionCount={}", uId, count);
        }
    }

    @Override
    public List<ChatUserVO> searchChatUsers(String keyword, Integer limit) {
        int size = (limit == null || limit <= 0) ? 20 : Math.min(limit, 50);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(User::getNickname, kw)
                    .or().like(User::getPhone, kw)
                    .or().like(User::getEmail, kw));
            // 纯数字时额外按 UID 精确匹配
            if (kw.length() <= 18 && kw.chars().allMatch(Character::isDigit)) {
                wrapper.or().eq(User::getUId, Long.valueOf(kw));
            }
        }
        wrapper.orderByDesc(User::getUId);

        List<User> records = userMapper.selectPage(new Page<>(1, size), wrapper).getRecords();
        return toVOList(records);
    }

    // ==================== 客户端应用登录支撑 ====================

    @Override
    public User getByUId(Long uId) {
        if (uId == null) {
            return null;
        }

        return userMapper.selectById(uId);
    }

    @Override
    public User getByOpenId(String openId) {
        if (StringUtils.isEmpty(openId)) {
            return null;
        }

        // 历史数据未对 wx_open_id 建唯一索引，理论上可能重复，这里限制 1 条避免 TooManyResultsException
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getWxOpenId, openId)
                .last("limit 1"));
    }

    @Override
    public User registerByOpenId(String openId) {
        User user = new User();

        user.setWxOpenId(openId);
        user.setNickname(DEFAULT_NICKNAME_PREFIX + StringUtils.maskOpenId(openId));
        // password 列 NOT NULL 且无默认值，微信用户无密码，写空串占位
        user.setPassword("");
        // phone / email 列上有唯一索引且默认空串，多个空串会互相冲突，必须留 null
        user.setPhone(null);
        user.setEmail(null);
        user.setSex("0");
        user.setStatus(STATUS_NORMAL);
        // 小程序用户默认未绑定聊天应用
        user.setIsBindChat(UNBIND_CHAT);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发首登：另一个请求已建号，回查后直接复用
            User exist = getByOpenId(openId);
            if (exist == null) {
                log.error("注册客户端用户失败，openId={}", StringUtils.maskOpenId(openId), e);
                throw new BusinessException("注册用户失败");
            }
            return exist;
        }

        return user;
    }

    // ==================== 内部方法 ====================

    private LambdaQueryWrapper<User> buildQueryWrapper(ChatUserQuery query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (query.getUId() != null) {
            wrapper.eq(User::getUId, query.getUId());
        }
        if (StringUtils.isNotEmpty(query.getNickname())) {
            wrapper.like(User::getNickname, query.getNickname());
        }
        if (StringUtils.isNotEmpty(query.getPhone())) {
            wrapper.like(User::getPhone, query.getPhone());
        }
        if (StringUtils.isNotEmpty(query.getEmail())) {
            wrapper.like(User::getEmail, query.getEmail());
        }
        if (StringUtils.isNotEmpty(query.getStatus())) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        if (StringUtils.isNotEmpty(query.getIsBindChat())) {
            wrapper.eq(User::getIsBindChat, query.getIsBindChat());
        }
        wrapper.orderByDesc(User::getUId);
        return wrapper;
    }

    /**
     * 按在线状态过滤分页
     *
     * <p>在线状态来自 Redis 登录会话，无法下推到 SQL，只能在内存中过滤。
     * 为兼顾总数与页码的准确性，逐批扫描并按需收集目标页数据，最多扫描
     * {@link #ONLINE_SCAN_MAX} 条；超过上限时总数以已扫描到的匹配数为准。</p>
     */
    private PageResult<ChatUserVO> pageByOnline(String online, int pageNum, int pageSize, LambdaQueryWrapper<User> wrapper) {
        boolean wantOnline = "1".equals(online);
        long skip = (long) (pageNum - 1) * pageSize;

        List<ChatUserVO> matched = new ArrayList<>();
        long total = 0;
        int current = 1;
        boolean finished = false;

        while (!finished) {
            Page<User> page = userMapper.selectPage(new Page<>(current, ONLINE_SCAN_BATCH), wrapper);
            List<User> records = page.getRecords();
            if (records.isEmpty()) {
                break;
            }

            Map<Long, Integer> onlineCounts = chatSessionService.countOnlineSessions(
                    records.stream().map(User::getUId).toList());

            for (User user : records) {
                int count = onlineCounts.getOrDefault(user.getUId(), 0);
                if ((count > 0) != wantOnline) {
                    continue;
                }
                if (total >= skip && matched.size() < pageSize) {
                    matched.add(buildVO(user, count));
                }
                total++;
            }

            current++;
            finished = records.size() < ONLINE_SCAN_BATCH || (long) current * ONLINE_SCAN_BATCH > ONLINE_SCAN_MAX;
        }

        return PageResult.of(matched, total, (long) pageNum, (long) pageSize);
    }

    private List<ChatUserVO> toVOList(List<User> users) {
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Integer> onlineCounts = chatSessionService.countOnlineSessions(
                users.stream().map(User::getUId).toList());

        List<ChatUserVO> list = new ArrayList<>(users.size());
        for (User user : users) {
            list.add(buildVO(user, onlineCounts.getOrDefault(user.getUId(), 0)));
        }
        return list;
    }

    private ChatUserVO buildVO(User user, int onlineDeviceCount) {
        ChatUserVO vo = new ChatUserVO();
        vo.setUId(user.getUId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setSex(user.getSex());
        vo.setIsBindChat(user.getIsBindChat());
        vo.setCreateTime(user.getCreateTime());
        vo.setOnlineDeviceCount(onlineDeviceCount);
        vo.setOnline(onlineDeviceCount > 0 ? "1" : "0");
        return vo;
    }
}
