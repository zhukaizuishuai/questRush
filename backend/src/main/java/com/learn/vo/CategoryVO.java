package com.learn.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树 VO（文档 6.2）
 */
@Data
public class CategoryVO {

    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;
    private LocalDateTime createTime;

    private List<CategoryVO> children = new ArrayList<>();
}
