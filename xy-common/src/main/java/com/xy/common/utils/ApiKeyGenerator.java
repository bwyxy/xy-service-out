package com.xy.common.utils;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key / Secret 安全生成器
 */
public class ApiKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String BASE62 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final String PREFIX = "xyai_";

    private ApiKeyGenerator() {
    }

    /**
     * 生成 API Key：前缀 + 32 字符 Base62，总长度 36
     */
    public static String generateApiKey() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(PREFIX);
        for (byte b : bytes) {
            int index = (b & 0xFF) % BASE62.length();
            sb.append(BASE62.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 生成 Secret Key：64 字节随机二进制，Base64 编码
     */
    public static String generateSecret() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
