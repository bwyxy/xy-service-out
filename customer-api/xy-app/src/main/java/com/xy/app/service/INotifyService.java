package com.xy.app.service;

import com.xy.app.pojo.dto.TodoSubscribeReportDTO;
import com.xy.app.pojo.vo.TodoNotifyQuotaVO;
import com.xy.app.response.WeChatApiResponse;
import com.xy.common.entity.todo.TodoNotifyLog;
import com.xy.common.entity.todo.TodoTask;

import java.time.LocalDateTime;

/**
 * 订阅消息提醒
 *
 * <p>一次性订阅的语义是「授权一次 = 可下发一条」，所以额度必须精确记账：
 * 授权时累加、下发前原子扣减，扣不动就不发。</p>
 * */
public interface INotifyService {

    /**
     * 任务提醒模板 Id（新建/编辑任务时写进提醒记录，调度时按它查额度）
     * */
    String getReminderTemplateId();

    /**
     * 上报订阅授权结果，返回最新额度
     * */
    TodoNotifyQuotaVO reportSubscribe(TodoSubscribeReportDTO dto);

    /**
     * 查当前用户的提醒额度
     * */
    TodoNotifyQuotaVO getQuota();

    /**
     * 扣减一次提醒额度
     *
     * @return false 表示额度已耗尽，本次不能下发
     * */
    boolean consumeReminderQuota(Long uId);

    /**
     * 下发一条任务提醒
     *
     * <p>只负责拼模板数据与调用微信，不判断该不该发（开关、免打扰、额度由调用方处理）。</p>
     *
     * @return 微信响应，errcode 为 0 表示下发成功
     * */
    WeChatApiResponse sendTaskReminder(String openid, TodoTask task, LocalDateTime remindAt);

    /**
     * 记一条下发日志（成功失败都要记，否则「用户说没收到」时无从排查）
     * */
    void writeLog(TodoNotifyLog notifyLog);
}
