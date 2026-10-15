package com.xy.system.dispatch.content;

import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.enums.DispatchContentFormat;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.system.dispatch.DispatchException;
import com.xy.system.dispatch.adapter.DispatchPlatformAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * 内容适配服务单元测试
 * 覆盖：Markdown 转换、图片保留、标题/摘要裁剪、标签去重裁剪、空内容与超长内容的异常处理
 * */
@ExtendWith(MockitoExtension.class)
class ContentAdaptServiceTest {

    @Mock
    private DispatchPlatformAdapter adapter;

    private final ContentAdaptService service = new ContentAdaptService();

    private static final String HTML = """
            <h1>多平台分发</h1>
            <p>这是<strong>正文</strong>内容。</p>
            <p><img src="https://cdn.example.com/a.png" alt="图" /></p>
            """;

    private BlogDispatchPlatform platform(Integer titleMax, Integer tagMax, Integer summaryMax, Integer contentMax) {
        BlogDispatchPlatform platform = new BlogDispatchPlatform();
        platform.setPlatformCode("juejin");
        platform.setPlatformName("掘金");
        platform.setTitleMaxLength(titleMax);
        platform.setTagMaxCount(tagMax);
        platform.setSummaryMaxLength(summaryMax);
        platform.setContentMaxLength(contentMax);
        return platform;
    }

    private void mockFormat(DispatchContentFormat format) {
        lenient().when(adapter.contentFormat()).thenReturn(format);
    }

    @Test
    void adapt_shouldConvertHtmlToMarkdownAndKeepImages() {
        mockFormat(DispatchContentFormat.MARKDOWN);

        AdaptedContent adapted = service.adapt("多平台分发", "摘要内容", HTML,
                Collections.emptyList(), platform(100, 5, 200, 100000), adapter);

        assertTrue(adapted.getContent().contains("# 多平台分发"), "标题应转为 Markdown 一级标题");
        assertTrue(adapted.getContent().contains("**正文**"), "加粗应转为 Markdown 语法");
        assertTrue(adapted.getContent().contains("![图](https://cdn.example.com/a.png)"), "图片应保留为 Markdown 图片语法");
        assertFalse(adapted.getContent().contains("<h1>"), "转换后不应残留 HTML 标签");
        assertTrue(adapted.getWarnings().isEmpty());
    }

    @Test
    void adapt_shouldFallbackSummaryFromContent_whenSummaryBlank() {
        mockFormat(DispatchContentFormat.MARKDOWN);

        AdaptedContent adapted = service.adapt("多平台分发", "   ", HTML,
                Collections.emptyList(), platform(100, 5, 200, 100000), adapter);

        assertTrue(adapted.getSummary().contains("多平台分发"), "摘要为空时应取正文纯文本");
        assertFalse(adapted.getSummary().contains("<h1>"), "回填摘要应为纯文本");
    }

    @Test
    void adapt_shouldTruncateTitleAndSummaryAndRecordWarning() {
        mockFormat(DispatchContentFormat.MARKDOWN);

        AdaptedContent adapted = service.adapt("这是一个非常长的文章标题需要被裁剪", "这是摘要", HTML,
                Collections.emptyList(), platform(5, 5, 2, 100000), adapter);

        assertEquals(5, adapted.getTitle().length());
        assertEquals(2, adapted.getSummary().length());
        assertEquals(2, adapted.getWarnings().size());
        assertTrue(adapted.getWarnings().get(0).contains("标题"));
    }

    @Test
    void adapt_shouldDeduplicateAndTrimTags() {
        mockFormat(DispatchContentFormat.MARKDOWN);

        List<String> tags = Arrays.asList("掘金", " 掘金 ", "后端", "", null, "Java");
        AdaptedContent adapted = service.adapt("多平台分发", "摘要", HTML, tags,
                platform(100, 2, 200, 100000), adapter);

        assertEquals(List.of("掘金", "后端"), adapted.getTags());
        assertEquals(1, adapted.getWarnings().size());
        assertTrue(adapted.getWarnings().get(0).contains("标签"));
    }

    @Test
    void adapt_shouldThrowContentViolation_whenTitleBlank() {
        mockFormat(DispatchContentFormat.MARKDOWN);

        DispatchException exception = assertThrows(DispatchException.class,
                () -> service.adapt("  ", "摘要", HTML, Collections.emptyList(),
                        platform(100, 5, 200, 100000), adapter));
        assertEquals(DispatchErrorTypeEnum.CONTENT_VIOLATION, exception.getErrorType());
    }

    @Test
    void adapt_shouldThrowContentViolation_whenContentBlank() {
        DispatchException exception = assertThrows(DispatchException.class,
                () -> service.adapt("标题", "摘要", "", Collections.emptyList(),
                        platform(100, 5, 200, 100000), adapter));
        assertEquals(DispatchErrorTypeEnum.CONTENT_VIOLATION, exception.getErrorType());
    }

    @Test
    void adapt_shouldThrowContentTooLarge_whenContentExceedsLimit() {
        mockFormat(DispatchContentFormat.MARKDOWN);

        DispatchException exception = assertThrows(DispatchException.class,
                () -> service.adapt("标题", "摘要", HTML, Collections.emptyList(),
                        platform(100, 5, 200, 10), adapter));
        assertEquals(DispatchErrorTypeEnum.CONTENT_TOO_LARGE, exception.getErrorType());
        assertTrue(exception.getMessage().contains("超出平台上限"));
    }

    @Test
    void adapt_shouldSanitizeScript_whenPlatformRequiresHtml() {
        mockFormat(DispatchContentFormat.HTML);

        String html = "<p>正文</p><script>alert(1)</script><style>.a{color:red}</style>";
        AdaptedContent adapted = service.adapt("标题", "摘要", html, Collections.emptyList(),
                platform(100, 5, 200, 100000), adapter);

        assertFalse(adapted.getContent().contains("alert(1)"));
        assertFalse(adapted.getContent().contains("color:red"));
        assertTrue(adapted.getContent().contains("正文"));
    }

    @Test
    void adapt_shouldProducePlainText_whenPlatformRequiresPlainText() {
        mockFormat(DispatchContentFormat.PLAIN_TEXT);

        AdaptedContent adapted = service.adapt("标题", "摘要", HTML, Collections.emptyList(),
                platform(100, 5, 200, 100000), adapter);

        assertFalse(adapted.getContent().contains("<"));
        assertTrue(adapted.getContent().contains("多平台分发"));
    }
}
