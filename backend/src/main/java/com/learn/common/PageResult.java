package com.learn.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 统一分页返回体 {list, total, pageNum, pageSize}（文档 6.0）
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long pageNum;
    private long pageSize;

    public static <T> PageResult<T> of(IPage<?> page, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.setList(list);
        r.setTotal(page.getTotal());
        r.setPageNum(page.getCurrent());
        r.setPageSize(page.getSize());
        return r;
    }

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.setList(list);
        r.setTotal(total);
        r.setPageNum(pageNum);
        r.setPageSize(pageSize);
        return r;
    }
}
