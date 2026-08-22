package com.xy.system.client.ai;

/**
 * xy-ai 服务内部调用客户端
 */
public interface AiServiceClient {

    /**
     * 调用 xy-ai 添加/更新知识库文档并触发分片与向量化
     */
    boolean syncDocument(Long kbId, String title, String sourceType, String sourceUrl, String content);

    /**
     * 调用 xy-ai 删除知识库文档及向量
     */
    boolean removeDocument(Long docId);
}
