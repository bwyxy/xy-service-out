package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.chat.LoginLog;
import com.xy.common.ip.AddressUtils;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.LoginLogMapper;
import com.xy.system.service.IUserLoginLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端用户登录日志服务实现
 * */
@Slf4j
@Service
public class UserLoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements IUserLoginLogService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void record(LoginLog loginLog) {
        if (loginLog == null) {
            return;
        }
        if (loginLog.getLoginTime() == null) {
            loginLog.setLoginTime(LocalDateTime.now());
        }
        // 客户端上报的设备指纹、上游返回的错误信息长度不可控，按库表长度截断，避免整条日志写入失败
        loginLog.setDeviceId(truncate(loginLog.getDeviceId(), 128));
        loginLog.setDeviceName(truncate(loginLog.getDeviceName(), 255));
        loginLog.setMsg(truncate(loginLog.getMsg(), 255));

        // 异步落库：登录接口对时延敏感，且地址解析依赖外部 HTTP 接口，均不能占用登录主链路
        CompletableFuture.runAsync(() -> {
            try {
                this.save(loginLog);
            } catch (Exception e) {
                log.error("记录客户端登录日志失败, account={}", loginLog.getAccount(), e);
                return;
            }
            fillLoginLocation(loginLog);
        });
    }

    @Override
    public PageResult<LoginLog> selectLoginLogPage(LoginLog query) {
        int pageNum = query.getPageNum() <= 0 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() <= 0 ? 10 : query.getPageSize();

        LambdaQueryWrapper<LoginLog> queryWrapper = new LambdaQueryWrapper<>();
        if (query.getUId() != null) {
            queryWrapper.eq(LoginLog::getUId, query.getUId());
        }
        if (StringUtils.isNotEmpty(query.getAccount())) {
            queryWrapper.like(LoginLog::getAccount, query.getAccount());
        }
        if (StringUtils.isNotEmpty(query.getIp())) {
            queryWrapper.like(LoginLog::getIp, query.getIp());
        }
        if (StringUtils.isNotEmpty(query.getStatus())) {
            queryWrapper.eq(LoginLog::getStatus, query.getStatus());
        }
        if (query.getDeviceType() != null) {
            queryWrapper.eq(LoginLog::getDeviceType, query.getDeviceType());
        }

        Map<String, Object> params = query.getParams();
        if (params != null) {
            String beginTime = (String) params.get("beginTime");
            String endTime = (String) params.get("endTime");
            if (StringUtils.isNotEmpty(beginTime)) {
                queryWrapper.ge(LoginLog::getLoginTime, parseTime(beginTime));
            }
            if (StringUtils.isNotEmpty(endTime)) {
                queryWrapper.le(LoginLog::getLoginTime, parseTime(endTime));
            }
        }

        queryWrapper.orderByDesc(LoginLog::getLoginTime);

        Page<LoginLog> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public void deleteLoginLogById(Long id) {
        getBaseMapper().deleteById(id);
    }

    /**
     * 回填登录地点
     * <p>AddressUtils 走外部 HTTP 查询且未设置超时，必须在异步线程内执行并兜底异常。</p>
     */
    private void fillLoginLocation(LoginLog loginLog) {
        String ip = loginLog.getIp();
        if (StringUtils.isEmpty(ip)) {
            return;
        }
        try {
            String location = AddressUtils.getRealAddressByIP(ip);
            if (StringUtils.isNotEmpty(location)) {
                LoginLog update = new LoginLog();
                update.setId(loginLog.getId());
                update.setLoginLocation(location);
                this.updateById(update);
            }
        } catch (Exception e) {
            log.warn("解析登录地点失败, ip={}", ip, e);
        }
    }

    private LocalDateTime parseTime(String time) {
        return LocalDateTime.parse(time, TIME_FORMATTER);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
