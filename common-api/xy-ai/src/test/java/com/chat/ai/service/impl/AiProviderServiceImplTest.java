package com.chat.ai.service.impl;

import com.chat.ai.dto.ModelProviderRequest;
import com.chat.ai.entity.AiModelProvider;
import com.chat.ai.exception.AiException;
import com.chat.ai.service.AiProviderService;
import com.chat.ai.vo.ModelProviderVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-test.sql")
@Transactional
class AiProviderServiceImplTest {

    @Autowired
    private AiProviderService providerService;

    @Test
    void shouldReturnDefaultProviderAfterInsertion() {
        ModelProviderRequest request = new ModelProviderRequest();
        request.setName("测试 OpenAI");
        request.setCode("openai");
        request.setType("OPENAI_COMPATIBLE");
        request.setEndpoint("http://localhost:9999");
        request.setApiKey("test-key");
        request.setModel("test-model");
        request.setPriority(10);
        request.setEnabled(1);

        assertTrue(providerService.saveOrUpdateProvider(request));

        AiModelProvider provider = providerService.getDefaultProvider();
        assertNotNull(provider);
        assertEquals("openai", provider.getCode());
    }

    @Test
    void shouldThrowWhenNoProviderEnabled() {
        assertThrows(AiException.class, () -> providerService.getDefaultProvider());
    }

    @Test
    void shouldListProvidersOrderedByPriority() {
        ModelProviderRequest r1 = new ModelProviderRequest();
        r1.setName("P1");
        r1.setCode("p1");
        r1.setType("OPENAI_COMPATIBLE");
        r1.setPriority(5);
        r1.setEnabled(1);
        providerService.saveOrUpdateProvider(r1);

        ModelProviderRequest r2 = new ModelProviderRequest();
        r2.setName("P2");
        r2.setCode("p2");
        r2.setType("OPENAI_COMPATIBLE");
        r2.setPriority(10);
        r2.setEnabled(1);
        providerService.saveOrUpdateProvider(r2);

        List<ModelProviderVo> list = providerService.listProviders();
        assertEquals(2, list.size());
        assertEquals("p2", list.get(0).getCode());
    }

    @Test
    void shouldToggleProviderStatus() {
        ModelProviderRequest request = new ModelProviderRequest();
        request.setName("测试");
        request.setCode("test");
        request.setType("OPENAI_COMPATIBLE");
        request.setEnabled(1);
        providerService.saveOrUpdateProvider(request);

        AiModelProvider inserted = providerService.getEnabledProvider("test");
        assertEquals(1, inserted.getEnabled().intValue());

        providerService.toggleProvider(inserted.getId());
        AiModelProvider updated = providerService.getById(inserted.getId());
        assertEquals(0, updated.getEnabled().intValue());
    }
}
