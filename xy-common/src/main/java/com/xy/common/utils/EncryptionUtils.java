package com.xy.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 通用加密工具类
 * 包含：BCrypt（密码哈希）、AES（对称加密）
 */
@Component
public class EncryptionUtils {

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 对明文进行 BCrypt哈希
     * */
    public String encode(CharSequence rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 校验明文与哈希是否匹配
     * */
    public boolean matches(CharSequence raw, String encoded) {
        return passwordEncoder.matches(raw, encoded);
    }

    private static final String AES_ALGORITHM = "AES";

    /**
     * 生成一个 AES密钥
     * */
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);

        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    /**
     * 将SecretKey转为Base64字符串（便于存储）
     */
    public static String secretKeyToBase64(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    /**
     * 从Base64字符串恢复SecretKey
     */
    public static SecretKey secretKeyFromBase64(String secretKey) {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);

        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

    /**
     * AES加密
     * @param plainText 明文
     * @param secretKey 密钥（SecretKey对象）
     * @return Base64编码的密文
     */
    public static String aesEncrypt(String plainText, SecretKey secretKey) throws Exception {

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * AES解密
     * @param cipherText Base64编码的密文
     * @param secretKey 密钥
     * @return 明文
     */
    public static String aesDecrypt(String cipherText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] encryptedBytes = Base64.getDecoder().decode(cipherText);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

}
