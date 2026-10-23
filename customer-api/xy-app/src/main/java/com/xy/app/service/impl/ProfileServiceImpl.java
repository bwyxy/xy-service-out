package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xy.app.pojo.dto.TodoProfileUpdateDTO;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.IProfileService;
import com.xy.common.entity.User;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 个人资料服务实现
 *
 * <p>直接操作 t_user：只更新本次传了值的字段，避免编辑页局部提交把其它字段覆盖成空。</p>
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements IProfileService {

    /**
     * 性别允许的取值，与 t_user.sex 的 char(1) 定义（0-未知 1-男 2-女）一致
     * */
    private static final Set<String> SEX_VALUES = Set.of("0", "1", "2");

    /**
     * 昵称长度上限，留出比 t_user.nickname varchar(50) 更紧的余量，避免超长被数据库截断
     * */
    private static final int NICKNAME_MAX_LENGTH = 20;

    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(TodoProfileUpdateDTO dto) {

        Long uId = TodoUserDetails.getCurrentUId();

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getUId, uId)
                .set(User::getUpdateTime, LocalDateTime.now());

        boolean changed = false;

        if (dto.getNickname() != null) {
            String nickname = dto.getNickname().trim();

            if (nickname.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "昵称不能为空");
            }

            if (nickname.length() > NICKNAME_MAX_LENGTH) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR,
                        "昵称不能超过" + NICKNAME_MAX_LENGTH + "个字");
            }

            wrapper.set(User::getNickname, nickname);
            changed = true;
        }

        if (dto.getAvatar() != null) {
            String avatar = dto.getAvatar().trim();

            if (avatar.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "头像地址不能为空");
            }

            wrapper.set(User::getAvatar, avatar);
            changed = true;
        }

        if (dto.getSex() != null) {
            String sex = dto.getSex().trim();

            if (!SEX_VALUES.contains(sex)) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "性别取值不合法");
            }

            wrapper.set(User::getSex, sex);
            changed = true;
        }

        if (!changed) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "没有需要修改的内容");
        }

        userMapper.update(null, wrapper);
    }
}
