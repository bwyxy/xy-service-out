package com.xy.common.constant;

public class Constants {
    public static final Integer SUCCESS = 200;
    public static final Integer FAIL = 500;
    public static final String UTF8 = "UTF-8";

    /**
     * www主域
     */
    public static final String WWW = "www.";

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 管理员角色权限标识
     */
    public static final String SUPER_ADMIN = "admin";

    /**
     * 角色权限分隔符
     */
    public static final String ROLE_DELIMITER = ",";

    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 令牌
     */
    public static final String TOKEN = "token";

    /**
     * 令牌前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 令牌前缀
     */
    public static final String LOGIN_USER_KEY = "login_user_key";

    /**
     * 群聊 ID前缀
     * */
    public static final String GROUP_ID_PREFIX = "g_";

    public static class HttpStatus {
        public static final Integer OK = 200;
        public static final Integer BAD_REQUEST = 400;
        public static final Integer UNAUTHORIZED = 401;
        public static final Integer FORBIDDEN = 403;
        public static final Integer NOT_FOUND = 404;
        public static final Integer INTERNAL_ERROR = 500;
    }
}
