package com.xy.system.dispatch.content;

import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.enums.DispatchContentFormat;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.system.dispatch.DispatchException;
import com.xy.system.dispatch.adapter.DispatchPlatformAdapter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 内容适配服务
 * 依据目标平台的正文格式与长度限制，把文章中心的富文本内容适配成平台可直接接收的形态：
 * 正文 HTML 转 Markdown / 纯文本、标题与摘要按平台上限裁剪、标签数量按平台上限裁剪
 * */
@Service
public class ContentAdaptService {

    /**
     * 执行内容适配
     * @param title 文章标题
     * @param summary 文章摘要
     * @param htmlContent 文章正文（文章中心存储的富文本 HTML）
     * @param tags 文章标签
     * @param platform 目标平台配置
     * @param adapter 目标平台适配器
     * */
    public AdaptedContent adapt(String title, String summary, String htmlContent,
                                List<String> tags, BlogDispatchPlatform platform,
                                DispatchPlatformAdapter adapter) {
        AdaptedContent result = new AdaptedContent();
        result.setTitle(adaptTitle(title, platform, result));
        result.setSummary(adaptSummary(summary, htmlContent, platform, result));
        result.setContent(adaptContent(htmlContent, platform, adapter));
        result.setTags(adaptTags(tags, platform, result));
        return result;
    }

    private String adaptTitle(String title, BlogDispatchPlatform platform, AdaptedContent result) {
        String value = title == null ? "" : title.trim();
        String adapted = truncate(value, platform.getTitleMaxLength(), "标题", result);
        if (adapted.isBlank()) {
            throw new DispatchException(DispatchErrorTypeEnum.CONTENT_VIOLATION, "文章标题为空，无法分发");
        }
        return adapted;
    }

    private String adaptSummary(String summary, String htmlContent, BlogDispatchPlatform platform,
                                AdaptedContent result) {
        String value = summary == null ? "" : summary.trim();
        if (value.isBlank()) {
            value = HtmlToMarkdownUtils.toPlainText(htmlContent).replaceAll("\\s+", " ").trim();
        }
        return truncate(value, platform.getSummaryMaxLength(), "摘要", result);
    }

    private String adaptContent(String htmlContent, BlogDispatchPlatform platform, DispatchPlatformAdapter adapter) {
        String html = htmlContent == null ? "" : htmlContent;
        if (html.isBlank()) {
            throw new DispatchException(DispatchErrorTypeEnum.CONTENT_VIOLATION, "文章正文为空，无法分发");
        }
        DispatchContentFormat format = adapter == null ? DispatchContentFormat.MARKDOWN : adapter.contentFormat();
        String content = switch (format) {
            case MARKDOWN -> HtmlToMarkdownUtils.convert(html);
            case PLAIN_TEXT -> HtmlToMarkdownUtils.toPlainText(html);
            default -> sanitizeHtml(html);
        };

        Integer maxLength = platform.getContentMaxLength();
        if (maxLength != null && maxLength > 0 && content.length() > maxLength) {
            throw new DispatchException(DispatchErrorTypeEnum.CONTENT_TOO_LARGE,
                    "正文长度 " + content.length() + " 超出平台上限 " + maxLength + "，请精简后重试");
        }
        return content;
    }

    private List<String> adaptTags(List<String> tags, BlogDispatchPlatform platform, AdaptedContent result) {
        List<String> adapted = new ArrayList<>();
        if (tags == null) {
            return adapted;
        }
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            String value = tag.trim();
            if (!adapted.contains(value)) {
                adapted.add(value);
            }
        }
        Integer maxCount = platform.getTagMaxCount();
        if (maxCount != null && maxCount > 0 && adapted.size() > maxCount) {
            result.getWarnings().add("标签数量超出平台上限，已保留前 " + maxCount + " 个");
            return new ArrayList<>(adapted.subList(0, maxCount));
        }
        return adapted;
    }

    private String truncate(String value, Integer maxLength, String fieldName, AdaptedContent result) {
        if (maxLength == null || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        result.getWarnings().add(fieldName + "超出平台上限 " + maxLength + " 字符，已自动裁剪");
        return value.substring(0, maxLength);
    }

    /**
     * 清理 HTML 中的脚本与样式，保留原有内联样式（微信等平台依赖内联样式渲染）
     * */
    private String sanitizeHtml(String html) {
        Document document = Jsoup.parse(html);
        document.select("script,style").remove();
        return document.body().html();
    }
}
