package com.xy.framework.web.service;

import com.xy.common.core.redis.RedisCache;
import com.xy.common.utils.VerifyCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VerifyService {

    private static final String REDIS_KET_PREFIX = "user:verify:code:";

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private MailService mailService;

    /**
     * 发送验证码
     * @param email 邮箱
     * */
    public void sendVerifyCode(String email) throws Exception {

        if (!VerifyCodeUtils.checkEmail(email)) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }

        String code = VerifyCodeUtils.generateVerifyCode();

        String key = REDIS_KET_PREFIX + email;

        log.warn("查询是否发送过验证码-{}", Optional.ofNullable(redisCache.hasKey(key)));
        if (Boolean.TRUE.equals(redisCache.hasKey(key))) {
            throw new Exception("发送验证码错误: 请您2分钟后重新发送!");
        } else {
            redisCache.setCacheObject(key, code, 2, TimeUnit.MINUTES);

            mailService.sendSimpleMail(email, code);
        }
    }

    /**
     * 校验验证码
     * @param email 邮箱
     * @param code 用户输入的验证码
     * @return 是否校验通过
     * */
    public boolean verifyCode(String email, String code){
        String key = REDIS_KET_PREFIX + email;

        String cachedCode = redisCache.getCacheObject(key);

        if (cachedCode != null && cachedCode.equals(code)) {
            // 验证通过后删除验证码
            redisCache.deleteObject(key);

            return true;
        }
        return false;
    }
}
