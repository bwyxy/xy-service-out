package com.xy.common.utils;

import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;

/**
 * 消息解密加密工具包
 * */
public class MsgEncryptUtils extends EncryptionUtils {

    @Value("${encrypt.key}")
    private String aesKey;

    /**
     * 解密
     * */
    public String decryptMsg(String content) {
        SecretKey secretKey = secretKeyFromBase64(aesKey);

        try {
            return aesDecrypt(content, secretKey);
        } catch (Exception e) {
            return content;
        }
    }

    /**
     * 加密消息
     * */
    public String encryptMsg(String content) {
        SecretKey secretKey = secretKeyFromBase64(aesKey);

        try {
            return aesEncrypt(content, secretKey);
        } catch (Exception e) {
            return content;
        }
    }
}
