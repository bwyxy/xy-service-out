package com.chat.ai.provider.deepseek;

import com.chat.ai.provider.openai.OpenAiModelProvider;
import com.chat.ai.service.AiProviderService;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 模型提供者实现
 *
 * <p>DeepSeek 提供 OpenAI 兼容接口，复用 OpenAiModelProvider 的调用逻辑。</p>
 */
@Component
public class DeepSeekModelProvider extends OpenAiModelProvider {

    public static final String CODE = "deepseek";

    public DeepSeekModelProvider(AiProviderService providerService) {
        super(providerService);
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    protected String getProviderCode() {
        return CODE;
    }
}
