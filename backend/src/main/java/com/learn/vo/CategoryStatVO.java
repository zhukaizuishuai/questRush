package com.learn.vo;

import lombok.Data;

/**
 * 按分类正确率统计 VO（文档 4.3：正确率基于流水表）
 */
@Data
public class CategoryStatVO {

    private Long categoryId;
    private String categoryName;
    private Long total;
    private Long correct;
}
