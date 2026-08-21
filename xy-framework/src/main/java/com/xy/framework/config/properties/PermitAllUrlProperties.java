package com.xy.framework.config.properties;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.xy.common.annotation.Anonymous;

/**
 * 配置白名单资源路径（仅收集 @Anonymous 注解的接口）
 */
@Component
@ConfigurationProperties(prefix = "security.ignore")
public class PermitAllUrlProperties {

    private final Set<String> whites = new HashSet<>();

    private volatile boolean initialized;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    public synchronized Set<String> getWhites() {
        if (!initialized) {
            collectAnonymousUrls();
            initialized = true;
        }
        return whites;
    }

    private void collectAnonymousUrls() {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        handlerMethods.forEach((info, handlerMethod) -> {
            // 判断方法或类上是否有 @Anonymous 注解
            boolean isAnonymous = false;
            // 检查方法上的注解
            if (handlerMethod.getMethod().isAnnotationPresent(Anonymous.class)) {
                isAnonymous = true;
            }
            // 检查类上的注解（如果方法上没有，再检查类）
            else if (handlerMethod.getBeanType().isAnnotationPresent(Anonymous.class)) {
                isAnonymous = true;
            }

            if (isAnonymous) {
                // 兼容 Spring Boot 2.x 和 3.x 获取路径模式
                Set<String> patterns = new HashSet<>();
                if (info.getPatternsCondition() != null) {
                    patterns = info.getPatternsCondition().getPatterns();
                } else if (info.getPathPatternsCondition() != null) {
                    patterns = info.getPathPatternsCondition().getPatterns().stream()
                            .map(org.springframework.web.util.pattern.PathPattern::getPatternString)
                            .collect(Collectors.toSet());
                }

                // 处理路径中的 {id} 占位符，转换为 *
                patterns.forEach(pattern -> {
                    String cleaned = pattern.replaceAll("\\{(.*?)\\}", "*");
                    whites.add(cleaned);
                });
            }
        });
    }
}