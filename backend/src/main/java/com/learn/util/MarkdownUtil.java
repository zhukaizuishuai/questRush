package com.learn.util;

import java.util.regex.Pattern;

/**
 * Markdown 轻量剥除：列表页题干摘要只需纯文本，不做完整渲染（渲染在前端完成，文档 3.0）。
 */
public final class MarkdownUtil {

    private static final Pattern CODE_FENCE = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]*)`");
    private static final Pattern IMAGE = Pattern.compile("!\\[.*?]\\(.*?\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^]]*)]\\([^)]*\\)");
    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s*", Pattern.MULTILINE);
    private static final Pattern EMPHASIS = Pattern.compile("[*_]{1,3}([^*_]*)[*_]{1,3}");
    private static final Pattern BLOCKQUOTE = Pattern.compile("^>\\s?", Pattern.MULTILINE);
    private static final Pattern LIST_MARK = Pattern.compile("^\\s*[-+*]\\s+", Pattern.MULTILINE);
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern MULTI_BLANK = Pattern.compile("\\n{2,}");

    private MarkdownUtil() {
    }

    /** 剥除常见 Markdown 语法，输出纯文本摘要 */
    public static String strip(String markdown) {
        if (markdown == null) {
            return "";
        }
        String s = markdown;
        s = CODE_FENCE.matcher(s).replaceAll(" ");
        s = IMAGE.matcher(s).replaceAll(" ");
        s = LINK.matcher(s).replaceAll("$1");
        s = INLINE_CODE.matcher(s).replaceAll("$1");
        s = HEADING.matcher(s).replaceAll("");
        s = EMPHASIS.matcher(s).replaceAll("$1");
        s = BLOCKQUOTE.matcher(s).replaceAll("");
        s = LIST_MARK.matcher(s).replaceAll("");
        s = HTML_TAG.matcher(s).replaceAll(" ");
        s = s.replace('\n', ' ').replace('\r', ' ');
        s = MULTI_BLANK.matcher(s).replaceAll(" ");
        return s.trim();
    }

    /** 生成列表页摘要：剥 Markdown + 截断 */
    public static String summary(String markdown, int maxLen) {
        String plain = strip(markdown);
        if (plain.length() <= maxLen) {
            return plain;
        }
        return plain.substring(0, maxLen) + "...";
    }
}
