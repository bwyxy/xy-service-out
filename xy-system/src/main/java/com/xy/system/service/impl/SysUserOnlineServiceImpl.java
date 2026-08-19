package com.xy.system.service.impl;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.xy.common.core.domain.model.LoginUser;
import com.xy.common.core.redis.RedisCache;
import com.xy.common.entity.SysUser;
import com.xy.common.entity.SysUserOnline;
import com.xy.common.ip.AddressUtils;
import com.xy.common.ip.IpUtils;
import com.xy.common.utils.StringUtils;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.common.web.service.TokenBlacklistService;
import com.xy.system.service.ISysUserOnlineService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 在线用户 服务实现层
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserOnlineServiceImpl implements ISysUserOnlineService {

    private static final String ONLINE_TOKEN_KEY = "online:token:";

    private static final String ONLINE_TOKEN_SET_KEY = "online:tokens";

    private final RedisCache redisCache;

    private final JwtTokenProvider jwtTokenProvider;

    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public void saveOnlineUser(LoginUser user, String token, HttpServletRequest request) {
        if (StringUtils.isEmpty(token)) {
            log.warn("保存在线用户失败，token 为空");
            return;
        }

        SysUser sysUser = user.getSysUser();
        if (sysUser == null) {
            log.warn("保存在线用户失败，登录用户实体为空");
            return;
        }

        String ipaddr = IpUtils.getIpAddr(request);
        UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        long loginTime = System.currentTimeMillis();
        long remainingTime = jwtTokenProvider.getRemainingExpiration(token);

        SysUserOnline online = new SysUserOnline();
        online.setTokenId(token);
        online.setUsername(user.getUsername());
        online.setDeptName("");
        online.setIpaddr(ipaddr);
        online.setLoginLocation(AddressUtils.getRealAddressByIP(ipaddr));
        online.setBrowser(userAgent.getBrowser().getName());
        online.setOs(userAgent.getOs().getName());
        online.setLoginTime(loginTime);
        online.setRoleName(joinRoles(sysUser.getRoles()));

        user.setToken(token);
        user.setIpaddr(ipaddr);
        user.setLoginLocation(online.getLoginLocation());
        user.setBrowser(online.getBrowser());
        user.setOs(online.getOs());
        user.setLoginTime(loginTime);
        user.setExpireTime(loginTime + remainingTime);

        int timeout = (int) Math.max(remainingTime / 1000, 1);
        redisCache.setCacheObject(ONLINE_TOKEN_KEY + token, online, timeout, TimeUnit.SECONDS);
        redisCache.addCacheSetMember(ONLINE_TOKEN_SET_KEY, token);
    }

    @Override
    public List<SysUserOnline> listOnlineUsers(String ipaddr, String username) {
        Collection<String> tokens = redisCache.getCacheSet(ONLINE_TOKEN_SET_KEY);
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysUserOnline> result = new ArrayList<>();
        for (String token : tokens) {
            SysUserOnline online = redisCache.getCacheObject(ONLINE_TOKEN_KEY + token);
            if (online == null) {
                redisCache.removeCacheSetMember(ONLINE_TOKEN_SET_KEY, token);
                continue;
            }
            if (StringUtils.isNotEmpty(ipaddr) && (online.getIpaddr() == null || !online.getIpaddr().contains(ipaddr))) {
                continue;
            }
            if (StringUtils.isNotEmpty(username) && (online.getUsername() == null || !online.getUsername().contains(username))) {
                continue;
            }
            result.add(online);
        }
        return result.stream()
                .sorted((o1, o2) -> Long.compare(o2.getLoginTime(), o1.getLoginTime()))
                .collect(Collectors.toList());
    }

    @Override
    public void forceLogout(String tokenId) {
        long remainingTime = jwtTokenProvider.getRemainingExpiration(tokenId);
        tokenBlacklistService.blacklist(tokenId, remainingTime);
        redisCache.deleteObject(ONLINE_TOKEN_KEY + tokenId);
        redisCache.removeCacheSetMember(ONLINE_TOKEN_SET_KEY, tokenId);
    }

    @Override
    public void removeOnlineUser(String tokenId) {
        if (StringUtils.isNotEmpty(tokenId)) {
            redisCache.deleteObject(ONLINE_TOKEN_KEY + tokenId);
            redisCache.removeCacheSetMember(ONLINE_TOKEN_SET_KEY, tokenId);
        }
    }

    private String joinRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return String.join(",", roles);
    }
}
