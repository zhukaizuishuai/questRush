package com.learn.util;

/**
 * 分页参数兜底。
 *
 * 背景：MyBatis-Plus 的 PaginationInnerInterceptor 在 size < 0 时**不拼 LIMIT**，
 * 直接 return，因此 `?pageSize=-1` 会一次性返回全表（且 count 被跳过、total=0）。
 * 仅用 Math.min(pageSize, 100) 无法拦住负数，必须同时保证下限。
 */
public final class PageUtil {

    private static final long MAX_SIZE = 100L;

    private PageUtil() {
    }

    /** 页码下限 1 */
    public static long num(long pageNum) {
        return pageNum < 1 ? 1 : pageNum;
    }

    /** 每页条数夹在 [1, 100] */
    public static long size(long pageSize) {
        if (pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, MAX_SIZE);
    }
}
