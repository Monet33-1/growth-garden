package com.grow.lyy.common.utils;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public final class MarkdownUtils {

    private static final Parser PARSER =
            Parser.builder().build();

    private static final HtmlRenderer RENDERER =
            HtmlRenderer.builder()
                    .escapeHtml(true)
                    .sanitizeUrls(true)
                    .build();

    private static final HtmlRenderer LINE_BREAK_RENDERER =
            HtmlRenderer.builder()
                    .escapeHtml(true)
                    .sanitizeUrls(true)
                    .softbreak("<br />\n")
                    .build();

    private MarkdownUtils() {
    }

    /**
     * Markdown 转 HTML
     */
    public static String toHtml(String markdown) {

        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        Node document = PARSER.parse(markdown);

        return RENDERER.render(document);
    }

    /** Markdown to safe HTML while preserving single line breaks, for authored letters. */
    public static String toHtmlPreservingLineBreaks(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        Node document = PARSER.parse(markdown);
        return LINE_BREAK_RENDERER.render(document);
    }

    /**
     * 获取纯文本
     */
    public static String toText(String markdown) {

        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        return markdown
                .replaceAll("(?m)^#{1,6}\\s*", "")      // 标题
                .replaceAll("(?m)^>\\s*", "")           // 引用
                .replaceAll("(?m)^[-*+]\\s*", "")       // 无序列表
                .replaceAll("(?m)^\\d+\\.\\s*", "")     // 有序列表
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1") // 粗体
                .replaceAll("\\*(.*?)\\*", "$1")       // 斜体
                .replaceAll("`(.*?)`", "$1")           // 行内代码
                .replaceAll("\\[(.*?)]\\((.*?)\\)", "$1")
                .trim();
    }

    /**
     * 获取摘要
     */
    public static String summary(
            String markdown,
            int length
    ) {

        String text = toText(markdown);

        if (text.length() <= length) {
            return text;
        }

        return text.substring(0, length) + "...";
    }

}
