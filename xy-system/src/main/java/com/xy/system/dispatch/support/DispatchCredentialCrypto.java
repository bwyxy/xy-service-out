package com.xy.system.dispatch.support;

import com.xy.common.exception.ServiceException;
import com.xy.common.utils.EncryptionUtils;
import com.xy.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 分发平台凭证加解密服务
 * 凭证以 AES 密文落库，任何情况下都不回传明文到前端
 * */
@Slf4j
@Component
public class DispatchCredentialCrypto {

    /**
     * 优先读取分发模块独立密钥，未配置时复用项目统一的 AES_KEY
     * */
    @Value("${dispatch.credential.key:${AES_KEY:}}")
    private String configuredKey;

    private volatile SecretKey cachedKey;

    /**
     * 加密凭证 JSON
     * */
    public String encrypt(String plainJson) {
        if (StringUtils.isEmpty(plainJson)) {
            return null;
        }
        try {
            return EncryptionUtils.aesEncrypt(plainJson, resolveKey());
        } catch (Exception e) {
            throw new ServiceException("分发凭证加密失败：" + e.getMessage());
        }
    }

    /**
     * 解密凭证 JSON，密文非法时返回 null 由调用方按凭证缺失处理
     * */
    public String decrypt(String cipherText) {
        if (StringUtils.isEmpty(cipherText)) {
            return null;
        }
        try {
            return EncryptionUtils.aesDecrypt(cipherText, resolveKey());
        } catch (Exception e) {
            log.warn("分发凭证解密失败，可能密钥已变更，密文长度={}", cipherText.length());
            return null;
        }
    }

    /**
     * 解析密钥：优先按 Base64 的 128/192/256 位密钥解析，否则用 SHA-256 派生 256 位密钥
     * 兼容不同部署环境里 AES_KEY 的书写方式
     * */
    private SecretKey resolveKey() {
        if (cachedKey != null) {
            return cachedKey;
        }
        synchronized (this) {
            if (cachedKey != null) {
                return cachedKey;
            }
            if (StringUtils.isEmpty(configuredKey)) {
                throw new ServiceException("未配置分发凭证加密密钥，请设置环境变量 DISPATCH_AES_KEY 或 AES_KEY");
            }
            cachedKey = buildKey(configuredKey.trim());
            return cachedKey;
        }
    }

    private SecretKey buildKey(String raw) {
        try {
            byte[] bytes = Base64.getDecoder().decode(raw);
            if (bytes.length == 16 || bytes.length == 24 || bytes.length == 32) {
                return new SecretKeySpec(bytes, "AES");
            }
        } catch (IllegalArgumentException ignored) {
            // 非 Base64 内容，走摘要派生
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new ServiceException("分发凭证加密密钥初始化失败：" + e.getMessage());
        }
    }
}
