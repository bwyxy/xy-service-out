package com.xy.system.dispatch.content;

import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.enums.DispatchContentFormat;
import com.xy.system.dispatch.adapter.DispatchPlatformAdapter;
import com.xy.system.dispatch.adapter.model.ImageUploadRequest;
import com.xy.system.dispatch.support.DispatchHttpClient;
import com.xy.system.utils.QiNiuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正文图片迁移服务
 * 优先把图片上传到目标平台的官方图床（适配器声明支持时），失败时对非 HTML 平台降级到自有七牛图床，
 * 均失败则保留原始地址并记录明细，避免因个别图片导致整篇分发失败
 * */
@Slf4j
@Service
public class ImageMigrationService {

    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\(([^)\\s]+)");

    private final DispatchHttpClient httpClient;

    private final QiNiuService qiNiuService;

    public ImageMigrationService(DispatchHttpClient httpClient, QiNiuService qiNiuService) {
        this.httpClient = httpClient;
        this.qiNiuService = qiNiuService;
    }

    /**
     * 迁移正文中的图片并重写图片地址
     * @param content 已适配格式的正文
     * @param format 正文字符串格式
     * @param credential 平台凭证（已解密）
     * @param config 平台发布默认规则
     * @param platform 平台配置
     * @param adapter 平台适配器，可空
     * @param timeoutSeconds 超时秒数
     * */
    public ImageMigrationResult migrate(String content, DispatchContentFormat format,
                                        Map<String, String> credential, Map<String, Object> config,
                                        BlogDispatchPlatform platform, DispatchPlatformAdapter adapter,
                                        int timeoutSeconds) {
        ImageMigrationResult result = new ImageMigrationResult();
        result.setContent(content == null ? "" : content);
        if (content == null || content.isBlank()) {
            return result;
        }

        List<String> urls = extractImageUrls(content);
        result.setTotalImages(urls.size());
        if (urls.isEmpty()) {
            return result;
        }

        String hostPattern = configOf(config, "imageHostPattern");
        int timeout = timeoutSeconds > 0 ? timeoutSeconds : 15;

        for (String url : urls) {
            if (shouldSkip(url, hostPattern)) {
                continue;
            }
            byte[] bytes;
            try {
                bytes = httpClient.download(url, timeout);
            } catch (Exception e) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getFailures().add(url + "：下载失败 - " + e.getMessage());
                continue;
            }

            String newUrl = uploadToPlatform(url, bytes, credential, config, platform, adapter, timeout);
            String source = "平台图床";
            if (newUrl == null && format != DispatchContentFormat.HTML) {
                newUrl = uploadToOwnStorage(url, bytes);
                source = "自有图床";
            }

            if (newUrl == null || newUrl.isBlank()) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getFailures().add(url + "：迁移失败，已保留原始地址");
                continue;
            }
            if ("平台图床".equals(source)) {
                result.setMigratedCount(result.getMigratedCount() + 1);
            } else {
                result.setFallbackCount(result.getFallbackCount() + 1);
            }
            result.getUrlMapping().put(url, newUrl);
        }

        String rewritten = result.getContent();
        for (Map.Entry<String, String> entry : result.getUrlMapping().entrySet()) {
            rewritten = rewritten.replace(entry.getKey(), entry.getValue());
        }
        result.setContent(rewritten);
        return result;
    }

    /**
     * 提取正文中的图片地址，同时兼容 Markdown 语法与内嵌 HTML img 标签
     * */
    private List<String> extractImageUrls(String content) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = MARKDOWN_IMAGE.matcher(content);
        while (matcher.find()) {
            addIfAbsent(urls, matcher.group(1));
        }
        for (String url : HtmlToMarkdownUtils.extractImageUrls(content)) {
            addIfAbsent(urls, url);
        }
        return urls;
    }

    private void addIfAbsent(List<String> urls, String url) {
        if (url != null && !url.isBlank() && !urls.contains(url)) {
            urls.add(url);
        }
    }

    private String uploadToPlatform(String url, byte[] bytes, Map<String, String> credential,
                                    Map<String, Object> config, BlogDispatchPlatform platform,
                                    DispatchPlatformAdapter adapter, int timeout) {
        if (adapter == null || !adapter.supportsImageUpload()) {
            return null;
        }
        try {
            ImageUploadRequest request = new ImageUploadRequest();
            request.setBytes(bytes);
            request.setFileName(fileNameOf(url));
            request.setCredential(credential);
            request.setConfig(config);
            request.setPlatform(platform);
            request.setTimeoutSeconds(timeout);
            String uploaded = adapter.uploadContentImage(request);
            return uploaded == null || uploaded.isBlank() ? null : uploaded;
        } catch (Exception e) {
            log.warn("图片迁移到平台【{}】图床失败 url={} err={}", adapter.platformName(), url, e.getMessage());
            return null;
        }
    }

    private String uploadToOwnStorage(String url, byte[] bytes) {
        try {
            return qiNiuService.upload(bytes, fileNameOf(url), null);
        } catch (Exception e) {
            log.warn("图片迁移到自有图床失败 url={} err={}", url, e.getMessage());
            return null;
        }
    }

    private boolean shouldSkip(String url, String hostPattern) {
        if (url == null || url.isBlank() || url.startsWith("data:")) {
            return true;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return true;
        }
        if (hostPattern == null || hostPattern.isBlank()) {
            return false;
        }
        for (String pattern : hostPattern.split(",")) {
            String item = pattern.trim();
            if (!item.isEmpty() && url.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private String configOf(Map<String, Object> config, String key) {
        Object value = config == null ? null : config.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String fileNameOf(String url) {
        String path = url.split("\\?")[0];
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        return name.isBlank() ? "image.jpg" : name;
    }
}
