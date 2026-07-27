package com.xy.common.utils;

import java.util.Random;

public class VerifyCodeUtils {

    private static final String codes = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int code_length = 6;

    /**
     * 生成6位数验证码
     * */
    public static String generateVerifyCode() {
        Random random = new Random();

        StringBuilder code = new StringBuilder(code_length);
        for (int i = 0; i < code_length; i++) {
            code.append(codes.charAt(random.nextInt(codes.length())));
        }
        return code.toString();
    }

    /**
     * 校验邮箱格式
     * */
    public static boolean checkEmail(String email) {
        if (email == null || StringUtils.isEmpty(email)) {
            return false;
        }

        String regex = "^\\w+((-\\w+)|(\\.\\w+))*\\@[A-Za-z0-9]+((\\.|-)[A-Za-z0-9]+)*\\.[A-Za-z0-9]+$";

        return email.matches(regex);
    }
}
