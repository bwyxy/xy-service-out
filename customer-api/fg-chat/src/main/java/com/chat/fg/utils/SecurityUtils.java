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

    /** 未登录/凭证异常时统一对外提示，避免透出底层异常细节 */
    private static final String UNAUTHORIZED_MESSAGE = "登录状态已失效，请重新登录";

    /**
     * 获取 UId
     * */
    public static Long getUId() {
        ChatUserDetails loginUser = getLoginUser();

        if (loginUser == null || loginUser.getUId() == null) {
            throw new ServiceException(Constants.HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
        }

        return loginUser.getUId();
    }

    /**
     * 获取登录用户
     * */
    public static ChatUserDetails getLoginUser() {
        try {
            Object principal = getAuthentication() == null ? null : getAuthentication().getPrincipal();

            if (!(principal instanceof ChatUserDetails)) {
                throw new ServiceException(Constants.HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
            }

            return (ChatUserDetails) principal;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(Constants.HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
        }
    }

    /**
     * 获取Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
