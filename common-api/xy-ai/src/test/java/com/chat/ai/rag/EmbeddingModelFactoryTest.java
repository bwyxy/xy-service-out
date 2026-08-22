package com.chat.ai.rag;

import com.xy.common.entity.ai.AiModelProvider;
import com.chat.ai.service.AiProviderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/**
 * EmbeddingModelFactory 单元测试：验证双轨配置路由逻辑
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingModelFactoryTest {

    @Mock
    private EmbeddingModel defaultEmbeddingModel;

    @Mock
    private AiProviderService providerService;

    @InjectMocks
    private EmbeddingModelFactory factory;

    /**
     * 未指定 providerCode 时，应返回全局默认 EmbeddingModel
     */
    @Test
    void shouldReturnDefaultModelWhenProviderCodeIsEmpty() {
        EmbeddingModel model = factory.getModel("", "text-embedding-v3");
        assertSame(defaultEmbeddingModel, model);

        model = factory.getModel(null, "text-embedding-v3");
        assertSame(defaultEmbeddingModel, model);

        verifyNoInteractions(providerService);
    }

    /**
     * 指定了 providerCode 时，应从数据库查询并创建对应的 EmbeddingModel
     */
    @Test
    void shouldCreateModelFromDatabaseProvider() {
        AiModelProvider provider = new AiModelProvider();
        provider.setCode("dashscope-embedding");
        provider.setEndpoint("https://dashscope.aliyuncs.com/compatible-mode/v1");
        provider.setApiKey("sk-test-key");
        provider.setModel("text-embedding-v3");
        provider.setEnabled(1);

        when(providerService.getEnabledProvider("dashscope-embedding")).thenReturn(provider);

        EmbeddingModel model = factory.getModel("dashscope-embedding", null);

        assertNotSame(defaultEmbeddingModel, model);
        verify(providerService).getEnabledProvider("dashscope-embedding");
    }

    /**
     * providerCode 对应的提供者不存在时，应降级为默认 EmbeddingModel
     */
    @Test
    void shouldFallbackToDefaultWhenProviderNotFound() {
        when(providerService.getEnabledProvider("not-exist"))
                .thenThrow(new RuntimeException("提供者不存在"));

        EmbeddingModel model = factory.getModel("not-exist", "text-embedding-v3");

        assertSame(defaultEmbeddingModel, model);
        verify(providerService).getEnabledProvider("not-exist");
    }

    /**
     * provider 存在但未配置 API Key 时，应降级为默认 EmbeddingModel
     */
    @Test
    void shouldFallbackToDefaultWhenProviderHasNoApiKey() {
        AiModelProvider provider = new AiModelProvider();
        provider.setCode("empty-key");
        provider.setEndpoint("https://example.com/v1");
        provider.setApiKey("");
        provider.setEnabled(1);

        when(providerService.getEnabledProvider("empty-key")).thenReturn(provider);

        EmbeddingModel model = factory.getModel("empty-key", "text-embedding-v3");

        assertSame(defaultEmbeddingModel, model);
    }
}
