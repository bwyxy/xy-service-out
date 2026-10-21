package com.xy.common.utils;

import com.xy.common.constant.Constants;

/**
 * 聊天频道 ID 解析工具
 *
 * <p>频道 ID 约定：群聊为 {@code g_{groupId}}。历史代码在多个 Service 中各自用
 * 正则 {@code \d+} 抓取其中的数字，遇到 {@code abc123}、{@code g_1_2} 这类输入会
 * 静默取到错误 ID，这里统一收敛为一处严格解析。</p>
 * */
public class ChannelIdUtils {

    private ChannelIdUtils() {
    }

    /**
     * 解析群 ID
     *
     * <p>同时兼容 {@code g_123}（频道 ID）与 {@code 123}（纯群 ID）两种入参；
     * 前缀后必须全为数字，否则返回 null，避免把非法输入解析成错误的群。</p>
     *
     * @param value 群频道 ID 或纯数字群 ID
     * @return 群 ID，格式非法时返回 null
     * */
    public static Long parseGroupId(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        String idPart = value.startsWith(Constants.GROUP_ID_PREFIX)
                ? value.substring(Constants.GROUP_ID_PREFIX.length())
                : value;

        if (idPart.isEmpty() || !isNumeric(idPart)) {
            return null;
        }

        try {
            return Long.valueOf(idPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 拼接群频道 ID
     * */
    public static String buildGroupChannelId(Long groupId) {
        return Constants.GROUP_ID_PREFIX + groupId;
    }

    private static boolean isNumeric(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
