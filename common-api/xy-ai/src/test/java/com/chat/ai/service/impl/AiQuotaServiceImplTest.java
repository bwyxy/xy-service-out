package com.chat.ai.service.impl;

import com.chat.ai.entity.AiUserQuota;
import com.chat.ai.exception.AiException;
import com.chat.ai.service.AiQuotaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-test.sql")
@Transactional
class AiQuotaServiceImplTest {

    @Autowired
    private AiQuotaService quotaService;

    @Test
    void shouldInitQuotaOnFirstConsume() {
        assertTrue(quotaService.consumeQuota(1L, "CLIENT"));
        AiUserQuota quota = quotaService.getOrInitQuota(1L, "CLIENT");
        assertEquals(1, quota.getDailyUsed().intValue());
        assertEquals(1L, quota.getTotalUsed().longValue());
    }

    @Test
    void shouldThrowWhenExceedDailyLimit() {
        AiUserQuota quota = quotaService.getOrInitQuota(2L, "CLIENT");
        quota.setDailyLimit(1);
        quota.setDailyUsed(1);
        quotaService.updateById(quota);

        assertThrows(AiException.class, () -> quotaService.consumeQuota(2L, "CLIENT"));
    }
}
