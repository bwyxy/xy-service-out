package com.chat.ai.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档分片器
 */
@Component
public class DocumentChunker {

    private static final int DEFAULT_CHUNK_SIZE = 500;

    private static final int DEFAULT_OVERLAP = 50;

    /**
     * 按字符数进行滑动窗口分片
     */
    public List<String> chunk(String text, Integer chunkSize, Integer overlap) {
        int size = chunkSize != null && chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;
        int lap = overlap != null && overlap >= 0 ? overlap : DEFAULT_OVERLAP;

        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        if (text.length() <= size) {
            chunks.add(text);
            return chunks;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            chunks.add(text.substring(start, end));
            start += size - lap;
            if (start >= end) {
                break;
            }
        }
        return chunks;
    }
}
