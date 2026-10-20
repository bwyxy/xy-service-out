package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.BaseChannel;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.User;
import com.xy.common.entity.chat.UserBlacklist;
import com.xy.common.enums.ChannelType;
import com.xy.common.vo.chat.UserBlacklistVO;
import com.xy.system.mapper.UserBlacklistMapper;
import com.xy.system.mapper.UserMapper;
import com.xy.system.service.IUserBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户黑名单服务实现
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBlacklistServiceImpl extends ServiceImpl<UserBlacklistMapper, UserBlacklist> implements IUserBlacklistService {

    private final UserMapper userMapper;
    private final WuKongClient wuKongClient;

    @Override
    public PageResult<UserBlacklistVO> selectBlacklistPage(Long uId, Long targetUid, int pageNum, int pageSize) {
        int current = pageNum <= 0 ? 1 : pageNum;
        int size = pageSize <= 0 ? 10 : pageSize;

        LambdaQueryWrapper<UserBlacklist> queryWrapper = new LambdaQueryWrapper<>();
        if (uId != null) {
            queryWrapper.eq(UserBlacklist::getUId, uId);
        }
        if (targetUid != null) {
            queryWrapper.eq(UserBlacklist::getTargetUid, targetUid);
        }
        queryWrapper.orderByDesc(UserBlacklist::getId);

        Page<UserBlacklist> page = this.page(new Page<>(current, size), queryWrapper);
        List<UserBlacklistVO> list = toVOList(page.getRecords());
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public void addBlacklist(Long uId, Long targetUid, String remark, String operator) {
        if (uId == null || targetUid == null) {
            throw new IllegalArgumentException("用户与目标用户不能为空");
        }
        if (uId.equals(targetUid)) {
            throw new IllegalArgumentException("不能将自己加入黑名单");
        }
        if (isBlocked(uId, targetUid)) {
            throw new IllegalArgumentException("该用户已在黑名单中");
        }

        UserBlacklist blacklist = new UserBlacklist();
        blacklist.setUId(uId);
        blacklist.setTargetUid(targetUid);
        blacklist.setRemark(remark);
        blacklist.setOperator(operator);
        this.save(blacklist);

        syncImBlackList(uId, targetUid, true);
    }

    @Override
    public void removeBlacklist(Long id) {
        if (id == null) {
            return;
        }
        UserBlacklist blacklist = this.getById(id);
        if (blacklist == null) {
            return;
        }
        this.removeById(id);

        syncImBlackList(blacklist.getUId(), blacklist.getTargetUid(), false);
    }

    @Override
    public List<Long> listBlockedUids(Long uId) {
        if (uId == null) {
            return Collections.emptyList();
        }
        List<UserBlacklist> list = this.list(new LambdaQueryWrapper<UserBlacklist>()
                .eq(UserBlacklist::getUId, uId));
        return list.stream().map(UserBlacklist::getTargetUid).collect(Collectors.toList());
    }

    @Override
    public boolean isBlocked(Long ownerUid, Long fromUid) {
        if (ownerUid == null || fromUid == null || ownerUid.equals(fromUid)) {
            return false;
        }
        return this.count(new LambdaQueryWrapper<UserBlacklist>()
                .eq(UserBlacklist::getUId, ownerUid)
                .eq(UserBlacklist::getTargetUid, fromUid)) > 0;
    }

    // ==================== 内部方法 ====================

    /**
     * 为黑名单记录补充双方昵称与手机号
     */
    private List<UserBlacklistVO> toVOList(List<UserBlacklist> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> uids = new HashSet<>();
        for (UserBlacklist record : records) {
            if (record.getUId() != null) {
                uids.add(record.getUId());
            }
            if (record.getTargetUid() != null) {
                uids.add(record.getTargetUid());
            }
        }

        Map<Long, User> userMap = uids.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(uids).stream()
                        .collect(Collectors.toMap(User::getUId, Function.identity(), (a, b) -> a));

        List<UserBlacklistVO> list = new ArrayList<>(records.size());
        for (UserBlacklist record : records) {
            UserBlacklistVO vo = new UserBlacklistVO();
            vo.setId(record.getId());
            vo.setUId(record.getUId());
            vo.setTargetUid(record.getTargetUid());
            vo.setRemark(record.getRemark());
            vo.setOperator(record.getOperator());
            vo.setCreateTime(record.getCreateTime());

            User owner = userMap.get(record.getUId());
            if (owner != null) {
                vo.setNickname(owner.getNickname());
            }
            User target = userMap.get(record.getTargetUid());
            if (target != null) {
                vo.setTargetNickname(target.getNickname());
                vo.setTargetPhone(target.getPhone());
            }
            list.add(vo);
        }
        return list;
    }

    /**
     * 同步悟空 IM 个人频道黑名单
     * <p>本地表已在应用层拦截消息，此处为兜底手段，失败仅告警不影响主流程。</p>
     */
    private void syncImBlackList(Long ownerUid, Long targetUid, boolean add) {
        if (ownerUid == null || targetUid == null) {
            return;
        }
        try {
            BaseChannel channel = new BaseChannel();
            channel.setChannelId(String.valueOf(ownerUid));
            channel.setChannelType(ChannelType.PERSONAL_CHANNEL.getCode());
            channel.setUids(List.of(String.valueOf(targetUid)));

            if (add) {
                wuKongClient.addBlackList(channel);
            } else {
                wuKongClient.removeBlackList(channel);
            }
        } catch (Exception e) {
            log.warn("同步悟空IM频道黑名单失败, ownerUid={}, targetUid={}, add={}", ownerUid, targetUid, add, e);
        }
    }
}
