package com.xy.system.dispatch.support;

import com.xy.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分发凭证加解密单元测试
 * 覆盖：密文落库不可逆可读、密钥变更容错、密钥缺失与非法密文的异常处理
 * */
class DispatchCredentialCryptoTest {

    /** Base64 编码的 128 位密钥（对应 0123456789abcdef） */
    private static final String BASE64_KEY = "MDEyMzQ1Njc4OWFiY2RlZg==";

    private DispatchCredentialCrypto crypto(String key) {
        DispatchCredentialCrypto target = new DispatchCredentialCrypto();
        ReflectionTestUtils.setField(target, "configuredKey", key);
        return target;
    }

    private DispatchCredentialCrypto cryptoWithKey() {
        return crypto(BASE64_KEY);
    }

    @Test
    void encryptThenDecrypt_shouldRoundTrip() {
        DispatchCredentialCrypto crypto = cryptoWithKey();
        String plain = "{\"cookie\":\"session=abc123\",\"account\":\"tester\"}";

        String cipher = crypto.encrypt(plain);
        assertNotNull(cipher);
        assertNotEquals(plain, cipher);
        assertFalse(cipher.contains("abc123"), "密文中不允许出现明文凭证");

        assertEquals(plain, crypto.decrypt(cipher));
    }

    @Test
    void decrypt_shouldReturnNull_whenKeyChanged() {
        String cipher = cryptoWithKey().encrypt("{\"cookie\":\"session=abc123\"}");

        DispatchCredentialCrypto other = crypto("b3RoZXIta2V5LTE2Ynl0");
        assertNull(other.decrypt(cipher), "密钥变更后无法解密时应返回 null 而不是抛异常");
    }

    @Test
    void encrypt_shouldReturnNull_whenPlainTextBlank() {
        DispatchCredentialCrypto crypto = cryptoWithKey();
        assertNull(crypto.encrypt(null));
        assertNull(crypto.encrypt(""));
        assertNull(crypto.decrypt(null));
        assertNull(crypto.decrypt(""));
    }

    @Test
    void decrypt_shouldReturnNull_whenCipherTextIllegal() {
        DispatchCredentialCrypto crypto = cryptoWithKey();
        assertNull(crypto.decrypt("not-a-valid-cipher"));
    }

    @Test
    void encrypt_shouldThrowServiceException_whenKeyMissing() {
        DispatchCredentialCrypto crypto = crypto("");
        ServiceException exception = assertThrows(ServiceException.class, () -> crypto.encrypt("{\"cookie\":\"x\"}"));
        assertTrue(exception.getMessage().contains("密钥"));
    }

    @Test
    void encrypt_shouldSupportRawStringKey() {
        DispatchCredentialCrypto crypto = crypto("my-blog-dispatch-secret-key");
        String plain = "{\"appSecret\":\"secret-value\"}";

        String cipher = crypto.encrypt(plain);
        assertNotNull(cipher);
        assertEquals(plain, crypto.decrypt(cipher));
    }
}
