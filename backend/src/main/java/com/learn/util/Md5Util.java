package com.learn.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * 题干归一化与 MD5：服务于 Excel 导入幂等查重（文档 4.4）。
 * 归一化规则：去除所有空白字符（空格/制表符/换行）后取 MD5。
 */
public final class Md5Util {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private Md5Util() {
    }

    public static String md5Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // JDK 必带 MD5，不可能走到这里
            throw new IllegalStateException(e);
        }
    }

    /** 题干去空白归一化 → MD5（查重键的一部分：分类 + 题干 MD5） */
    public static String titleMd5(String title) {
        return md5Hex(WHITESPACE.matcher(title == null ? "" : title.trim()).replaceAll(""));
    }
}
