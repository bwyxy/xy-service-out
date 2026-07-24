package com.xy.common.utils;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文字内容审核工具类
 */
public class ContentCheckUtils {

    private static final int DEFAULT_MAX_LENGTH = 5000;
    private static final int DEFAULT_MIN_LENGTH = 1;

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "(?i)<script[^>]*>[\\s\\S]*?</script[^>]*>|<[^>]*on\\w+\\s*=|javascript:|vbscript:|expression\\s*\\(|eval\\s*\\("
    );
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(\\b(select|insert|update|delete|drop|alter|create|truncate|exec|execute|union|declare)\\b\\s)|('\\s*(or|and)\\s+.*?=)"
    );
    private static final Pattern CONSECUTIVE_SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+={}\\[\\]|\\\\:;\"'<>,.?/~`]{5,}");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(https?://|ftp://|www\\.)[^\\s]+"
    );
    private static final Pattern ONLY_SPECIAL_OR_NUMBER_PATTERN = Pattern.compile("^[\\d\\W_]+$");

    private static final SensitiveWordBs SENSITIVE_WORD_BS = SensitiveWordBs.newInstance()
            .ignoreCase(true)
            .ignoreWidth(true)
            .ignoreNumStyle(true)
            .ignoreChineseStyle(true)
            .ignoreEnglishStyle(true)
            .ignoreRepeat(true)
            .wordResultCondition(WordResultConditions.alwaysTrue())
            .init();

    /**
     * 获取 SensitiveWordBs 实例，用于自定义操作
     */
    public static SensitiveWordBs getSensitiveWordBs() {
        return SENSITIVE_WORD_BS;
    }

    /**
     * 添加自定义敏感词
     */
    public static void addSensitiveWords(List<String> words) {
        if (words != null && !words.isEmpty()) {
            SENSITIVE_WORD_BS.addWord(words);
        }
    }

    /**
     * 添加自定义敏感词
     */
    public static void addSensitiveWords(String... words) {
        if (words != null && words.length > 0) {
            SENSITIVE_WORD_BS.addWord(Arrays.asList(words));
        }
    }

    /**
     * 移除敏感词
     */
    public static void removeSensitiveWords(String... words) {
        if (words != null && words.length > 0) {
            SENSITIVE_WORD_BS.removeWord(Arrays.asList(words));
        }
    }

    /**
     * 综合内容审核（使用默认长度限制）
     */
    public static void check(String content) {
        check(content, DEFAULT_MIN_LENGTH, DEFAULT_MAX_LENGTH);
    }

    /**
     * 综合内容审核
     */
    public static void check(String content, int minLength, int maxLength) {
        checkEmpty(content);
        checkLength(content, minLength, maxLength);
        checkSensitiveWord(content);
        checkScript(content);
        checkSqlInjection(content);
    }

    /**
     * 文章内容审核（标题 + 正文 + 摘要）
     */
    public static void checkArticle(String title, String content, String summary) {
        checkEmpty(title);
        checkLength(title, 1, 200);
        checkSensitiveWord(title);

        if (StringUtils.isNotEmpty(summary)) {
            checkLength(summary, 1, 500);
            checkSensitiveWord(summary);
        }

        if (StringUtils.isNotEmpty(content)) {
            checkLength(content, 1, 50000);
            checkSensitiveWord(content);
            checkScript(content);
        }
    }

    /**
     * 检查是否为空
     */
    public static void checkEmpty(String content) {
        if (StringUtils.isEmpty(content)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "内容不能为空");
        }
    }

    /**
     * 检查内容长度
     */
    public static void checkLength(String content, int minLength, int maxLength) {
        if (content == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "内容不能为空");
        }
        int length = content.trim().length();
        if (length < minLength) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR,
                    String.format("内容长度不能少于%d个字符", minLength));
        }
        if (length > maxLength) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR,
                    String.format("内容长度不能超过%d个字符", maxLength));
        }
    }

    /**
     * 敏感词检测
     */
    public static void checkSensitiveWord(String content) {
        if (StringUtils.isEmpty(content)) {
            return;
        }
        if (SENSITIVE_WORD_BS.contains(content)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "内容包含违规信息，请修改后重试");
        }
    }

    /**
     * 检测是否包含敏感词
     */
    public static boolean containsSensitiveWord(String content) {
        if (StringUtils.isEmpty(content)) {
            return false;
        }
        return SENSITIVE_WORD_BS.contains(content);
    }

    /**
     * 获取内容中匹配到的敏感词列表
     */
    public static List<String> getSensitiveWords(String content) {
        if (StringUtils.isEmpty(content)) {
            return List.of();
        }
        return SENSITIVE_WORD_BS.findAll(content);
    }

    /**
     * 获取第一个匹配到的敏感词
     */
    public static String findFirstSensitiveWord(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        return SENSITIVE_WORD_BS.findFirst(content);
    }

    /**
     * 对敏感词进行脱敏替换
     */
    public static String replaceSensitiveWords(String content) {
        if (StringUtils.isEmpty(content)) {
            return "";
        }
        return SENSITIVE_WORD_BS.replace(content);
    }

    /**
     * 检测是否包含脚本注入
     */
    public static void checkScript(String content) {
        if (StringUtils.isEmpty(content)) {
            return;
        }
        if (SCRIPT_PATTERN.matcher(content).find()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "内容包含非法脚本，请修改后重试");
        }
    }

    /**
     * 检测是否包含SQL注入
     */
    public static void checkSqlInjection(String content) {
        if (StringUtils.isEmpty(content)) {
            return;
        }
        if (SQL_INJECTION_PATTERN.matcher(content).find()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "内容包含非法字符，请修改后重试");
        }
    }

    /**
     * 检测是否包含连续特殊字符（刷屏字符）
     */
    public static void checkConsecutiveSpecialChars(String content) {
        if (StringUtils.isEmpty(content)) {
            return;
        }
        if (CONSECUTIVE_SPECIAL_PATTERN.matcher(content).find()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "内容包含过多特殊字符，请修改后重试");
        }
    }

    /**
     * 检测是否包含外部链接
     */
    public static void checkExternalLink(String content) {
        if (StringUtils.isEmpty(content)) {
            return;
        }
        if (URL_PATTERN.matcher(content).find()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "内容不允许包含外部链接");
        }
    }

    /**
     * 检测是否仅包含特殊字符或数字（无意义内容）
     */
    public static void checkMeaningful(String content) {
        if (StringUtils.isEmpty(content)) {
            return;
        }
        if (ONLY_SPECIAL_OR_NUMBER_PATTERN.matcher(content.trim()).matches()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "内容不合法，请输入有意义的文字");
        }
    }

    /**
     * 去除HTML标签
     */
    public static String removeHtmlTags(String content) {
        if (StringUtils.isEmpty(content)) {
            return "";
        }
        return HTML_TAG_PATTERN.matcher(content).replaceAll("");
    }

    /**
     * 对内容进行过滤（去除脚本和HTML标签），返回安全的纯文本
     */
    public static String sanitize(String content) {
        if (StringUtils.isEmpty(content)) {
            return "";
        }
        String result = content;
        result = SCRIPT_PATTERN.matcher(result).replaceAll("");
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        return result.trim();
    }
}
