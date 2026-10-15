package com.xy.system.dispatch.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * HTML 富文本转 Markdown 工具
 * 针对文章中心 wangEditor 产出的标签集合实现，覆盖标题、段落、加粗/斜体/删除线/下划线、
 * 行内代码与代码块、引用、有序/无序列表（含嵌套）、链接、图片、表格、分割线
 * */
public final class HtmlToMarkdownUtils {

    private HtmlToMarkdownUtils() {
    }

    /**
     * 富文本 HTML 转 Markdown
     * */
    public static String convert(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        Element body = Jsoup.parse(html).body();
        StringBuilder sb = new StringBuilder();
        for (Node node : body.childNodes()) {
            renderNode(node, sb, 0);
        }
        return normalize(sb.toString());
    }

    /**
     * 富文本 HTML 转纯文本
     * */
    public static String toPlainText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    /**
     * 去除 HTML 中的图片节点，用于不需要图片的平台
     * */
    public static String stripImages(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        Element body = Jsoup.parse(html).body();
        body.select("img").remove();
        return body.html();
    }

    /**
     * 提取 HTML 中所有图片地址（保持出现顺序，去重）
     * */
    public static List<String> extractImageUrls(String html) {
        List<String> urls = new ArrayList<>();
        if (html == null || html.isEmpty()) {
            return urls;
        }
        Elements images = Jsoup.parse(html).body().select("img");
        for (Element img : images) {
            String src = img.attr("src");
            if (src != null && !src.isBlank() && !urls.contains(src)) {
                urls.add(src);
            }
        }
        return urls;
    }

    private static void renderNode(Node node, StringBuilder sb, int listDepth) {
        if (node instanceof TextNode textNode) {
            String text = textNode.getWholeText().replaceAll("\\s+", " ");
            if (!text.isBlank()) {
                sb.append(text);
            }
            return;
        }
        if (node instanceof Element element) {
            renderElement(element, sb, listDepth);
        }
    }

    private static void renderElement(Element element, StringBuilder sb, int listDepth) {
        String tag = element.tagName();
        switch (tag) {
            case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                ensureBlankLine(sb);
                sb.append("#".repeat(Integer.parseInt(tag.substring(1)))).append(' ');
                renderChildren(element, sb, listDepth);
                appendNewlines(sb, 2);
            }
            case "p" -> {
                ensureBlankLine(sb);
                renderChildren(element, sb, listDepth);
                appendNewlines(sb, 2);
            }
            case "div", "section", "article", "header", "footer", "main" -> {
                ensureBlankLine(sb);
                renderChildren(element, sb, listDepth);
                appendNewlines(sb, 2);
            }
            case "br" -> sb.append("\n");
            case "hr" -> {
                ensureBlankLine(sb);
                sb.append("---");
                appendNewlines(sb, 2);
            }
            case "strong", "b" -> {
                sb.append("**");
                renderChildren(element, sb, listDepth);
                sb.append("**");
            }
            case "em", "i" -> {
                sb.append("*");
                renderChildren(element, sb, listDepth);
                sb.append("*");
            }
            case "u" -> {
                sb.append("<u>");
                renderChildren(element, sb, listDepth);
                sb.append("</u>");
            }
            case "s", "del", "strike" -> {
                sb.append("~~");
                renderChildren(element, sb, listDepth);
                sb.append("~~");
            }
            case "code" -> sb.append('`').append(element.wholeText().replace("`", "\\`")).append('`');
            case "pre" -> renderCodeBlock(element, sb);
            case "blockquote" -> renderBlockquote(element, sb, listDepth);
            case "ul" -> renderList(element, sb, listDepth, false);
            case "ol" -> renderList(element, sb, listDepth, true);
            case "a" -> renderLink(element, sb, listDepth);
            case "img" -> renderImage(element, sb);
            case "table" -> renderTable(element, sb);
            case "script", "style" -> {
                // 忽略脚本与样式
            }
            default -> renderChildren(element, sb, listDepth);
        }
    }

    private static void renderChildren(Element element, StringBuilder sb, int listDepth) {
        for (Node node : element.childNodes()) {
            renderNode(node, sb, listDepth);
        }
    }

    private static void renderCodeBlock(Element pre, StringBuilder sb) {
        Element codeEl = pre.selectFirst("code");
        String language = "";
        String code;
        if (codeEl != null) {
            code = codeEl.wholeText();
            language = resolveLanguage(codeEl);
        } else {
            code = pre.wholeText();
        }
        ensureBlankLine(sb);
        sb.append("```").append(language).append('\n');
        sb.append(code.stripTrailing()).append('\n');
        sb.append("```");
        appendNewlines(sb, 2);
    }

    private static String resolveLanguage(Element codeEl) {
        String className = codeEl.className();
        if (className == null || className.isBlank()) {
            return "";
        }
        for (String part : className.split("\\s+")) {
            if (part.startsWith("language-")) {
                return part.substring("language-".length());
            }
        }
        return "";
    }

    private static void renderBlockquote(Element element, StringBuilder sb, int listDepth) {
        ensureBlankLine(sb);
        StringBuilder inner = new StringBuilder();
        renderChildren(element, inner, listDepth);
        String content = inner.toString().strip();
        for (String line : content.split("\n")) {
            sb.append("> ").append(line).append('\n');
        }
        appendNewlines(sb, 2);
    }

    private static void renderList(Element listEl, StringBuilder sb, int listDepth, boolean ordered) {
        ensureBlankLine(sb);
        String indent = "  ".repeat(listDepth);
        int index = 1;
        for (Element li : listEl.children()) {
            if (!"li".equals(li.tagName())) {
                continue;
            }
            StringBuilder itemText = new StringBuilder();
            List<Element> subLists = new ArrayList<>();
            for (Node node : li.childNodes()) {
                if (node instanceof Element child
                        && ("ul".equals(child.tagName()) || "ol".equals(child.tagName()))) {
                    subLists.add(child);
                } else {
                    renderNode(node, itemText, listDepth + 1);
                }
            }
            String prefix = ordered ? (index++) + ". " : "- ";
            sb.append(indent).append(prefix).append(itemText.toString().strip()).append('\n');
            for (Element sub : subLists) {
                renderList(sub, sb, listDepth + 1, "ol".equals(sub.tagName()));
            }
        }
        appendNewlines(sb, 2);
    }

    private static void renderLink(Element element, StringBuilder sb, int listDepth) {
        String href = element.attr("href");
        StringBuilder text = new StringBuilder();
        renderChildren(element, text, listDepth);
        String label = text.toString().strip();
        if (href == null || href.isBlank()) {
            sb.append(label);
            return;
        }
        if (label.isEmpty()) {
            label = href;
        }
        sb.append('[').append(label).append("](").append(href).append(')');
    }

    private static void renderImage(Element element, StringBuilder sb) {
        String src = element.attr("src");
        if (src == null || src.isBlank()) {
            return;
        }
        String alt = element.attr("alt");
        ensureBlankLine(sb);
        sb.append("![").append(alt == null ? "" : alt).append("](").append(src).append(')');
        appendNewlines(sb, 2);
    }

    private static void renderTable(Element table, StringBuilder sb) {
        Elements rows = table.select("tr");
        if (rows.isEmpty()) {
            return;
        }
        List<List<String>> matrix = new ArrayList<>();
        int maxColumns = 0;
        for (Element row : rows) {
            List<String> cells = new ArrayList<>();
            for (Element cell : row.select("th,td")) {
                cells.add(cell.text().trim().replace("|", "\\|"));
            }
            maxColumns = Math.max(maxColumns, cells.size());
            matrix.add(cells);
        }
        if (maxColumns == 0) {
            return;
        }
        ensureBlankLine(sb);
        sb.append(buildTableRow(matrix.get(0), maxColumns)).append('\n');
        List<String> divider = new ArrayList<>();
        for (int i = 0; i < maxColumns; i++) {
            divider.add("---");
        }
        sb.append(buildTableRow(divider, maxColumns)).append('\n');
        for (int i = 1; i < matrix.size(); i++) {
            sb.append(buildTableRow(matrix.get(i), maxColumns)).append('\n');
        }
        appendNewlines(sb, 2);
    }

    private static String buildTableRow(List<String> cells, int maxColumns) {
        List<String> normalized = new ArrayList<>(cells);
        while (normalized.size() < maxColumns) {
            normalized.add("");
        }
        return "| " + String.join(" | ", normalized) + " |";
    }

    private static void ensureBlankLine(StringBuilder sb) {
        stripTrailingSpaces(sb);
        if (sb.length() == 0) {
            return;
        }
        appendNewlines(sb, 2);
    }

    private static void appendNewlines(StringBuilder sb, int count) {
        stripTrailingSpaces(sb);
        int trailing = 0;
        for (int i = sb.length() - 1; i >= 0 && sb.charAt(i) == '\n'; i--) {
            trailing++;
        }
        for (int i = trailing; i < count; i++) {
            sb.append('\n');
        }
    }

    private static void stripTrailingSpaces(StringBuilder sb) {
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.deleteCharAt(sb.length() - 1);
        }
    }

    private static String normalize(String markdown) {
        String result = markdown.replaceAll("[ \t]+\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .strip();
        return result;
    }
}
