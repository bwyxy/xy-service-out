package com.todo.common.utils;

import com.todo.common.constant.Constants;
import com.todo.common.exception.ServiceException;
import com.todo.common.core.domain.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 * */
public class SecurityUtils {

    /**
     * 用户ID
     **/
    private static Long getUserId() {
        try {
            return getLoginUser().getUserId();
        } catch (Exception e) {
            throw new ServiceException(Constants.HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * 获取用户名称
     * */
    private static String getUserName() {
        try {
            return getLoginUser().getUsername();
        } catch (Exception e) {
            throw new ServiceException(Constants.HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * 获取用户
     * */
    public static LoginUser getLoginUser() {
        try {
            return (LoginUser) getAuthentication().getPrincipal();
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

    /**
     * 是否为管理员
     *
     * @return 结果
     */
    public static boolean isAdmin() {
        return isAdmin(getUserId());
    }

    /**
     * 是否为管理员
     *
     * @param userId 用户ID
     * @return 结果
     */
    public static boolean isAdmin(Long userId)
    {
        return userId != null && 10000000L == userId;
    }
}
