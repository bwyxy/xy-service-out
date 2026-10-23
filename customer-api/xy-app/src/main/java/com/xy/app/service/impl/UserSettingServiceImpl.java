package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.app.mapper.UserSettingMapper;
import com.xy.app.pojo.dto.TodoUserSettingUpdateDTO;
import com.xy.app.pojo.vo.TodoUserSettingVO;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.IUserSettingService;
import com.xy.common.entity.todo.TodoUserSetting;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户提醒偏好服务实现
 * */
@Slf4j
@Service
public class UserSettingServiceImpl extends ServiceImpl<UserSettingMapper, TodoUserSetting> implements IUserSettingService {

    /** HH:mm 格式校验 */
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    /** 默认任务提醒开 */
    private static final int DEFAULT_TASK_REMIND_ON = 1;

    /** 默认每日提醒关 */
    private static final int DEFAULT_DAILY_REMIND_ON = 0;

    /** 默认每日提醒时间 */
    private static final String DEFAULT_DAILY_REMIND_TIME = "08:00";

    /** 默认番茄提醒开 */
    private static final int DEFAULT_FOCUS_REMIND_ON = 1;

    /** 默认免打扰开始 */
    private static final String DEFAULT_QUIET_START = "22:00";

    /** 默认免打扰结束 */
    private static final String DEFAULT_QUIET_END = "07:00";

    @Override
    public TodoUserSettingVO getSetting() {
        return toVO(getOrDefault(TodoUserDetails.getCurrentUId()));
    }

    @Override
    public void updateSetting(TodoUserSettingUpdateDTO dto) {

        Long uId = TodoUserDetails.getCurrentUId();

        // 时间字段允许不传（表示不改），传了就必须合法，否则调度器解析会炸
        validateTime(dto.getDailyRemindTime(), "每日提醒时间");
        validateTime(dto.getQuietStart(), "免打扰开始时间");
        validateTime(dto.getQuietEnd(), "免打扰结束时间");

        if (getById(uId) == null) {
            insertSetting(uId, dto);

            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 只把「传了值」的字段 set 进去：设置页分多次保存开关和时间时不会互相覆盖
        LambdaUpdateWrapper<TodoUserSetting> wrapper = new LambdaUpdateWrapper<TodoUserSetting>()
                .eq(TodoUserSetting::getUserId, uId)
                .set(TodoUserSetting::getUpdateTime, now);

        if (dto.getTaskRemindOn() != null) {
            wrapper.set(TodoUserSetting::getTaskRemindOn, normalizeSwitch(dto.getTaskRemindOn()));
        }

        if (dto.getDailyRemindOn() != null) {
            wrapper.set(TodoUserSetting::getDailyRemindOn, normalizeSwitch(dto.getDailyRemindOn()));
        }

        if (StringUtils.isNotBlank(dto.getDailyRemindTime())) {
            wrapper.set(TodoUserSetting::getDailyRemindTime, dto.getDailyRemindTime());
        }

        if (dto.getFocusRemindOn() != null) {
            wrapper.set(TodoUserSetting::getFocusRemindOn, normalizeSwitch(dto.getFocusRemindOn()));
        }

        if (StringUtils.isNotBlank(dto.getQuietStart())) {
            wrapper.set(TodoUserSetting::getQuietStart, dto.getQuietStart());
        }

        if (StringUtils.isNotBlank(dto.getQuietEnd())) {
            wrapper.set(TodoUserSetting::getQuietEnd, dto.getQuietEnd());
        }

        update(wrapper);
    }

    @Override
    public TodoUserSetting getOrDefault(Long uId) {

        TodoUserSetting setting = uId == null ? null : getById(uId);

        if (setting != null) {
            return setting;
        }

        return buildDefault(uId);
    }

    /**
     * 首次修改时落一条记录，未传的字段取默认值
     * */
    private void insertSetting(Long uId, TodoUserSettingUpdateDTO dto) {

        LocalDateTime now = LocalDateTime.now();

        TodoUserSetting setting = buildDefault(uId);
        setting.setCreateTime(now);
        setting.setUpdateTime(now);

        if (dto.getTaskRemindOn() != null) {
            setting.setTaskRemindOn(normalizeSwitch(dto.getTaskRemindOn()));
        }

        if (dto.getDailyRemindOn() != null) {
            setting.setDailyRemindOn(normalizeSwitch(dto.getDailyRemindOn()));
        }

        if (StringUtils.isNotBlank(dto.getDailyRemindTime())) {
            setting.setDailyRemindTime(dto.getDailyRemindTime());
        }

        if (dto.getFocusRemindOn() != null) {
            setting.setFocusRemindOn(normalizeSwitch(dto.getFocusRemindOn()));
        }

        if (StringUtils.isNotBlank(dto.getQuietStart())) {
            setting.setQuietStart(dto.getQuietStart());
        }

        if (StringUtils.isNotBlank(dto.getQuietEnd())) {
            setting.setQuietEnd(dto.getQuietEnd());
        }

        save(setting);
    }

    /**
     * 一份默认设置（不落库）
     * */
    private TodoUserSetting buildDefault(Long uId) {
        TodoUserSetting setting = new TodoUserSetting();
        setting.setUserId(uId);
        setting.setTaskRemindOn(DEFAULT_TASK_REMIND_ON);
        setting.setDailyRemindOn(DEFAULT_DAILY_REMIND_ON);
        setting.setDailyRemindTime(DEFAULT_DAILY_REMIND_TIME);
        setting.setFocusRemindOn(DEFAULT_FOCUS_REMIND_ON);
        setting.setQuietStart(DEFAULT_QUIET_START);
        setting.setQuietEnd(DEFAULT_QUIET_END);

        return setting;
    }

    private TodoUserSettingVO toVO(TodoUserSetting setting) {
        TodoUserSettingVO vo = new TodoUserSettingVO();
        vo.setTaskRemindOn(setting.getTaskRemindOn());
        vo.setDailyRemindOn(setting.getDailyRemindOn());
        vo.setDailyRemindTime(setting.getDailyRemindTime());
        vo.setFocusRemindOn(setting.getFocusRemindOn());
        vo.setQuietStart(setting.getQuietStart());
        vo.setQuietEnd(setting.getQuietEnd());

        return vo;
    }

    /**
     * 开关只接受 0/1，其余值一律当关，避免脏值让调度器判断失真
     * */
    private int normalizeSwitch(Integer value) {
        return value != null && value == 1 ? 1 : 0;
    }

    private void validateTime(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            return;
        }

        if (!TIME_PATTERN.matcher(value).matches()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, fieldName + "格式不正确，应为 HH:mm");
        }
    }
}
