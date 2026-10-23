package com.xy.app.service.impl;

import com.xy.app.client.WeChatApiClient;
import com.xy.app.config.WeChatSubscribeProperties;
import com.xy.app.mapper.NotifyLogMapper;
import com.xy.app.mapper.NotifyQuotaMapper;
import com.xy.app.pojo.dto.TodoSubscribeReportDTO;
import com.xy.app.pojo.vo.TodoNotifyQuotaVO;
import com.xy.app.response.WeChatApiResponse;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.INotifyService;
import com.xy.common.entity.todo.TodoNotifyLog;
import com.xy.common.entity.todo.TodoSubscribeQuota;
import com.xy.common.entity.todo.TodoTask;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 订阅消息提醒
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements INotifyService {

    /**
     * 微信 thing 类型字段上限 20 个字符，超了直接返回 47003，所以本地先截断
     * */
    private static final int THING_MAX_LENGTH = 20;

    /**
     * 微信 time 类型要求年月日 + 时分
     * */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm");

    /**
     * thing 类型不允许空值，备注为空时用占位
     * */
    private static final String EMPTY_TEXT = "无";

    /**
     * 单次上报允许的最大授权次数，防刷
     * */
    private static final int MAX_REPORT_COUNT = 50;

    private final NotifyQuotaMapper notifyQuotaMapper;

    private final NotifyLogMapper notifyLogMapper;

    private final WeChatApiClient weChatApiClient;

    private final WeChatSubscribeProperties subscribeProperties;

    @Override
    public String getReminderTemplateId() {
        return subscribeProperties.getReminderTemplateId();
    }

    @Override
    public TodoNotifyQuotaVO reportSubscribe(TodoSubscribeReportDTO dto) {
        Long uId = TodoUserDetails.getCurrentUId();
        String templateId = resolveTemplateId(dto == null ? null : dto.getTemplateId());
        int count = resolveReportCount(dto == null ? null : dto.getCount());

        if (count > 0) {
            notifyQuotaMapper.addQuota(uId, templateId, count);
        }

        return buildQuotaVO(uId, templateId);
    }

    @Override
    public TodoNotifyQuotaVO getQuota() {
        return buildQuotaVO(TodoUserDetails.getCurrentUId(), getReminderTemplateId());
    }

    @Override
    public boolean consumeReminderQuota(Long uId) {
        return notifyQuotaMapper.consumeQuota(uId, getReminderTemplateId()) > 0;
    }

    @Override
    public WeChatApiResponse sendTaskReminder(String openid, TodoTask task, LocalDateTime remindAt) {
        return weChatApiClient.sendSubscribeMessage(
                openid,
                getReminderTemplateId(),
                subscribeProperties.getPage(),
                buildData(task, remindAt));
    }

    @Override
    public void writeLog(TodoNotifyLog notifyLog) {
        try {
            notifyLogMapper.insert(notifyLog);
        } catch (Exception e) {
            // 日志写不进去不该影响下发结果，只记一笔本地日志
            log.warn("写订阅消息下发日志失败 err={}", e.getMessage());
        }
    }

    /**
     * 拼模板数据
     *
     * <p>字段 key 全部取自配置，换模板时只改 application.yml。</p>
     * */
    private Map<String, Map<String, String>> buildData(TodoTask task, LocalDateTime remindAt) {
        // remindAt 由 dueDate 减去提前量得来，理论上 dueDate 必不为空，兜底用 remindAt
        LocalDateTime dueDate = task.getDueDate() == null ? remindAt : task.getDueDate();

        Map<String, Map<String, String>> data = new HashMap<>();
        data.put(subscribeProperties.getTitleKey(), value(clip(task.getTaskTitle())));
        data.put(subscribeProperties.getStartTimeKey(), value(TIME_FORMATTER.format(dueDate)));
        data.put(subscribeProperties.getMemoKey(), value(clip(task.getDescription())));
        data.put(subscribeProperties.getTipKey(), value(clip(subscribeProperties.getTipText())));
        data.put(subscribeProperties.getRemindTimeKey(), value(TIME_FORMATTER.format(remindAt)));

        return data;
    }

    /**
     * 模板数据统一是 { "value": "…" } 结构
     * */
    private Map<String, String> value(String text) {
        Map<String, String> item = new HashMap<>();
        item.put("value", text);

        return item;
    }

    /**
     * thing 类型字段的限制：20 字以内、不能为空、不能带换行
     * */
    private String clip(String text) {
        if (StringUtils.isBlank(text)) {
            return EMPTY_TEXT;
        }

        String flat = text.replaceAll("[\\r\\n]+", " ").trim();

        if (StringUtils.isBlank(flat)) {
            return EMPTY_TEXT;
        }

        return flat.length() > THING_MAX_LENGTH ? flat.substring(0, THING_MAX_LENGTH) : flat;
    }

    /**
     * 只认服务端配置的模板，避免前端指定别的模板凭空刷额度
     * */
    private String resolveTemplateId(String templateId) {
        String configured = getReminderTemplateId();

        if (StringUtils.isBlank(templateId)) {
            return configured;
        }

        if (!templateId.equals(configured)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "订阅消息模板 Id 不匹配");
        }

        return configured;
    }

    /**
     * 授权次数只接受 0 ~ 上限的正数
     * */
    private int resolveReportCount(Integer count) {
        if (count == null || count <= 0) {
            return 0;
        }

        return Math.min(count, MAX_REPORT_COUNT);
    }

    /**
     * 查额度并组装返回，没有记录视为 0
     * */
    private TodoNotifyQuotaVO buildQuotaVO(Long uId, String templateId) {
        TodoSubscribeQuota quota = notifyQuotaMapper.selectQuota(uId, templateId);

        TodoNotifyQuotaVO vo = new TodoNotifyQuotaVO();
        vo.setTemplateId(templateId);
        vo.setRemain(quota == null || quota.getRemain() == null ? 0 : quota.getRemain());
        vo.setTotal(quota == null || quota.getTotal() == null ? 0 : quota.getTotal());

        return vo;
    }
}
