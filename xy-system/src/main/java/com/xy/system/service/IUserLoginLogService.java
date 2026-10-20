package com.xy.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.chat.LoginLog;

/**
 * 客户端用户登录日志服务接口
 *
 * <p>与后台账号登录日志（{@code sys_logininfor}）相互独立，仅服务聊天端用户（t_user）。</p>
 */
public interface IUserLoginLogService extends IService<LoginLog> {

    /**
     * 记录一条客户端登录日志（异步落库，不阻断登录主流程）
     *
     * @param loginLog 登录日志（IP、设备、浏览器等由调用方填充）
     */
    void record(LoginLog loginLog);

    /**
     * 分页查询登录日志
     *
     * @param query 查询条件（账号/UID/IP/状态/时间范围）
     * @return 分页结果
     */
    PageResult<LoginLog> selectLoginLogPage(LoginLog query);

    /**
     * 删除登录日志
     *
     * @param id 日志主键
     */
    void deleteLoginLogById(Long id);
}
