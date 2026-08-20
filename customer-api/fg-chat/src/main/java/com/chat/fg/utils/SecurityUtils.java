package com.chat.fg.utils;

import com.chat.fg.security.ChatUserDetails;
import com.xy.common.constant.Constants;
import com.xy.common.exception.ServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * chat-api 安全工具类
 * */
public class SecurityUtils {

    /**
     * 获取 UId
     * */
    public static Long getUId() {
        try {
            return getLoginUser().getUId();
        } catch (Exception e) {
            throw new ServiceException(Constants.HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * 获取登录用户
     * */
    public static ChatUserDetails getLoginUser() {
        try {
            return (ChatUserDetails) getAuthentication().getPrincipal();
        } catch (Exception e) {
            throw new ServiceException(Constants.HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * 获取Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
